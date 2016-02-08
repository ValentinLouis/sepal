package org.openforis.sepal.component.sandboxmanager

import groovy.json.JsonOutput
import groovy.transform.ToString
import groovyx.net.http.RESTClient
import org.openforis.sepal.SepalConfiguration
import org.openforis.sepal.hostingservice.Status
import org.openforis.sepal.hostingservice.WorkerInstance
import org.openforis.sepal.util.Clock
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static groovyx.net.http.ContentType.JSON

@ToString
class DockerSandboxSessionProvider implements SandboxSessionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(this)
    private final SepalConfiguration config
    private final Clock clock

    DockerSandboxSessionProvider(SepalConfiguration config, Clock clock) {
        this.config = config
        this.clock = clock
    }

    SandboxSession deploy(SandboxSession session, WorkerInstance instance) {
        try {
            LOG.info("Deploying $session to $instance")
            createContainer(session, instance)
            startContainer(session, instance)
            def port = determineContainerPort(session, instance)
            def deployedSession = session.deployed(Status.ACTIVE, instance, port, clock.now())
            waitUntilInitialized(deployedSession, instance)
            LOG.info("Deployed $session to $instance")
            return deployedSession
        } catch (Exception e) {
            LOG.error("Failed to deploy $session to $instance. Rolling back", e)
            rollbackFailedDeployment(session, instance)
            throw e
        }
    }

    void undeploy(SandboxSession session) {
        removeContainer(session, session.host)
    }

    void assertAvailable(SandboxSession session) throws SandboxSessionProvider.NotAvailable {
        def data = loadContainerInfo(session, session.host)
        if (!data.State.Running)
            throw new SandboxSessionProvider.NotAvailable(session.id, "Session not available: $session")
    }

    private void createContainer(SandboxSession session, WorkerInstance instance) {
        def request = new JsonOutput().toJson([
                Image: config.dockerImageName,
                Tty: true,
                Cmd: ["/start", session.username, config.ldapHost, config.ldapPassword],
                HostConfig: [
                        Binds: [
                                "$config.mountingHomeDir/$session.username:/home/$session.username",
                                "$config.publicHomeDir:/sepal/public",
                                "/data/sepal/certificates/ldap-ca.crt.pem:/etc/ldap/certificates/ldap-ca.crt.pem"
                        ],
                        PublishAllPorts: true],
                ExposedPorts: ['22/tcp': [:]]

        ])
        LOG.debug("Deploying session $session to $instance.")
        def response = client(instance).post(
                path: "containers/create",
                query: [name: containerName(session)],
                body: request,
                requestContentType: JSON
        )
        LOG.debug("Deployed session $session to $instance.")
        if (response.data.Warnings)
            LOG.warn("Warning when creating docker container for session $session in $instance: $response.data.Warnings")
    }

    private void startContainer(SandboxSession session, WorkerInstance instance) {
        def request = new JsonOutput().toJson([PublishAllPorts: true])
        client(instance).post(
                path: "containers/${containerName(session)}/start",
                body: request,
                requestContentType: JSON
        )
    }

    private void waitUntilInitialized(SandboxSession session, WorkerInstance instance) {
        LOG.debug("Waiting for session to be initialized on ports $config.sandboxPortsToCheck. " +
                "Session: $session, WorkerInstance: $instance")
        def response = client(instance).post(
                path: "containers/${containerName(session)}/exec",
                body: new JsonOutput().toJson([
                        AttachStdin: false,
                        AttachStdout: true,
                        AttachStderr: true,
                        Tty: false,
                        Cmd: ["/root/wait_until_initialized.sh", config.sandboxPortsToCheck]
                ]),
                requestContentType: JSON
        )
        def execId = response.data.Id
        client(instance).post(
                path: "exec/$execId/start",
                body: new JsonOutput().toJson([Detach: false, Tty: true]),
                requestContentType: JSON
        )
        LOG.debug("Session initialized. Session: $session, WorkerInstance: $instance.")
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    private int determineContainerPort(SandboxSession session, WorkerInstance instance) {
        def data = loadContainerInfo(session, instance.host)
        return Integer.parseInt(data.NetworkSettings.Ports["22/tcp"][0].HostPort)
    }

    private void removeContainer(SandboxSession session, String host) {
        LOG.info("Removing container for session on host $host. Session: $session")
        client(host).delete(path: "containers/${containerName(session)}", query: [force: true])
        LOG.info("Removed container for session on host $host. Session: $session")
    }

    private String containerName(SandboxSession session) {
        "sandbox-$session.username-$session.id"
    }

    private Map loadContainerInfo(SandboxSession session, String host) {
        client(host).get(path: "containers/${containerName(session)}/json").data
    }

    private RESTClient client(WorkerInstance instance) {
        client(instance.host)
    }

    private RESTClient client(String host) {
        new RESTClient("http://$host:$config.dockerDaemonPort/$config.dockerRESTEntryPoint/")
    }

    private void rollbackFailedDeployment(SandboxSession session, WorkerInstance instance) {
        try {
            removeContainer(session, instance.host)
        } catch (Exception ignore) {
            LOG.warn("Failed to rollback container after failed deployment of session $session to instance $instance")
        }
    }
}