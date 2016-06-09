package org.openforis.sepal.component.hostingservice.vagrant

import org.openforis.sepal.component.workerinstance.api.InstanceProvider
import org.openforis.sepal.component.workerinstance.api.WorkerInstance
import org.openforis.sepal.component.workersession.api.InstanceType

class VagrantInstanceProvider implements InstanceProvider {
    private static final HOST = '172.28.128.3'
    private final InstanceType instanceType
    private WorkerInstance instance
    private final List<Closure> launchListeners = []

    VagrantInstanceProvider(InstanceType instanceType) {
        this.instanceType = instanceType
        launchIdle([new WorkerInstance(
                id: 'vagrant-box',
                type: instanceType.id
        )])
    }

    WorkerInstance launchReserved(WorkerInstance instance) {
        this.instance = instance.release().launched(HOST, new Date()).reserve(instance.reservation).running()
        launchListeners*.call(this.instance)
        return this.instance
    }

    void launchIdle(List<WorkerInstance> instances) {
        if (instances)
            instance = instances.first().launched(HOST, new Date()).running()
    }

    void terminate(String instanceId) {
        if (instance.id == instanceId)
            instance = null
    }

    void reserve(WorkerInstance instance) {
        if (this.instance.id == instance.id)
            this.instance.reserve(instance.reservation)
    }

    void release(String instanceId) {
        if (instance.id == instanceId)
            instance = instance.release()
    }

    List<WorkerInstance> idleInstances(String instanceType) {
        [instance]
                .findAll { instance.type == instanceType }
                .findAll { !instance.reservation }
    }

    List<WorkerInstance> idleInstances() {
        [instance]
                .findAll { !instance.reservation }
    }

    List<WorkerInstance> reservedInstances() {
        [instance]
                .findAll { instance.reservation }
    }

    WorkerInstance getInstance(String instanceId) {
        instance.id == instanceId ? instance : null
    }

    void onInstanceLaunched(Closure listener) {
        launchListeners << listener
    }

    void start() {
        // Nothing to start
    }

    void stop() {
        // Nothing to stop
    }
}