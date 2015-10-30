SEPAL
=====
SEPAL is a cloud computing platform for geographical data processing. It enables users to quickly process large amount
of data without high network bandwidth requirements or need to invest in high-performance computing infrastructure.

Background
----------
Reducing Emissions from Deforestation and Forest Degradation (REDD) is an effort to create a financial value for the
carbon stored in forests, offering incentives for developing countries to reduce emissions from forested lands and
invest in low-carbon paths to sustainable development. "REDD+" goes beyond deforestation and forest degradation,
and includes the role of conservation, sustainable management of forests and enhancement of forest carbon stocks. The
UN-REDD Programme is the United Nations collaborative initiative on Reducing Emissions from Deforestation and forest
Degradation (REDD) in developing countries.

[FAO](http://www.fao.org/home/en/), as a member of the [UN-REDD Programme](http://www.un-redd.org/), is responsible for
assisting countries in developing robust national forest
monitoring systems (NFMS) and operational satellite land monitoring systems (SLMS) to help them to meet the measurement,
reporting and verification  (MRV) requirements of the REDD+.  Furtermore, countries need help in establishing and
maintaining  a SLMS capable of producing the information required to make consequential decisions about forest
management; decisions that promote sustainable forest management and can potentially mitigate the effects of global
climate change on society.  Specifically, a solution is needed to address the existing challenges countries face when
developing forest monitoring systems, due to difficulties accessing and processing remotely sensed data; a key source
of information for monitoring forest area and forest area change over large, often remote areas.

To tackle the problems mentioned above, FAO and Norway are collaborating on the System for Earth Observation
Data Access, Processing and Analysis for Land Monitoring (SEPAL).

It  consists of the following components:

1. A powerful, cloud-based computing platform for big data processing and storage.
2. Facilitated access to remote sensing data sources through a direct-access-interface to earth observation data repositories.
3. A set of open-source software tools, capable of efficient data processing
4. Related capacity development and technology transfer activities

The computing platform enables FAO national partners to process data quickly without locally maintained high
performance computing infrastructures.  The direct link to data repositories allows fast access to satellite
imagery and other earth observation data for processing.  The software tools, such as FAO’s
[Open Foris Geospatial Toolkit](http://www.openforis.org/tools/geospatial-toolkit.html)
perform powerful image processing, are completely customizable and function similarly ‘on the cloud’ or on the desktop.


Architectural overview
----------------------
The core of the system is the _SEPAL server_ and the _user sandboxes_. SEPAL server provides a web-based user-interface,
where geospatial data from multiple providers can be searched, processing chains composed and executed, and geospatial
data products visualized.

The user sandboxes are spaces where users get access to a number of geospatial data processing tools, such as those
included in Open Foris Geospatial Toolkit and Orfeo Toolbox, and their own dedicated storage. SEPAL provides users SSH
access to their respective sandbox. This can either be done directly with an SSH client, or through a provided web-based
terminal. Web-based sandbox tools can be accessed over HTTP.

Sandboxes are implemented as Docker containers, which in addition to providing isolation between users, allows for very
flexible deployment. Sandboxes are started when needed, and stopped when not used. This enables them to be deployed in a
cluster of worker server instances, which can be dynamically scaled up and down based on demand. It also allows the end
users to use the system independently on their own AWS account.

![alt tag](https://raw.githubusercontent.com/openforis/sepal/master/docs/Components.png)

### Components and services part of a SEPAL deployment

**HAProxy** -
Off-the-shelf load balancer, allowing SEPAL to be clustered for availability. Run both SSH and HTTPS on port 443,
to prevent firewalls from blocking SSH.

**nginx** -
Off-the-shelf HTTP and reverse proxy server, proxying all SEPAL HTTP endpoints.

**GeoServer** -
Off-the-shelf server for viewing and editing geospatial data. Used in SEPAL to visualize user
generated data products.

**GateOne** -
Off-the-shelf web-based SSH client. Gives  users SSH access to their Sandbox in a web browser.

**Sepal server** -
Provides the system user interface.

**Data provider** -
Service retrieving geospatial data from various external data providers.

**Sandbox lifecycle manager** -
Service managing the user sandboxes. It deploys them on demand when users requests access, and un-deploys them as soon
as a user disconnects from them.

**Sandbox SSH gateway** -
Service responsible for dynamically tunnelling SSH connections to users sandbox, while notifying the sandbox lifecycle
manager on connects and disconnects.

**Sandbox web proxy** -
Service proxying HTTP connections to user sandboxes. It maintains HTTP sessions, and notifies the sandbox lifecycle
manager on session creation and expiry.

**Sandbox** -
The user sandboxes are spaces where users get access to a number of geospatial data processing tools. See table below
for provided tools.


### Software deployed on each users sandbox:

**Open Foris Geospatial Toolkit** -
A collection of command-line utilities for processing of geospatial data.

**GDAL** -
A translator library for raster and vector geospatial data formats.

**Arcsi** -
A command line tool for the atmospheric correction of Earth Observation imagery.

**R** -
Language for statistical computing and graphics.

**RStudio Server** -
An IDE for R in a web browser.

**Tuiview** -
A lightweight raster GIS.

**QGIS** -
A free and open source geographic information system.

**Orfeo ToolBox** -
Library for remote sensing image processing.

Build and Release
-----------------
The project is under active development, and the build and release process is still in flux, so these
instructions will change, and improve, over time.

### Prerequisites
In order to build and run the SEPAL system, a Linux or Windows installation is needed. While it might be possible,
with significant effort, to do this on Windows, it is not supported. The end-users on the other hand, are of course
free to use whatever Operating system they prefer, including Windows.

In addition to this, the following software must be installed:

[Java](http://www.oracle.com/technetwork/java/javase/downloads/index.html),
[Maven](https://maven.apache.org/download.cgi), and
[Ansible](http://docs.ansible.com/ansible/intro_installation.html).
If you want to run the system locally, you need [Vagrant](https://www.vagrantup.com/downloads.html), and
to deploy on Amazon Web Services EC2 instances, you need an [AWS account](https://aws.amazon.com/account/).

### Configuration
TBD

### Build
TBD

### Deploy
TBD