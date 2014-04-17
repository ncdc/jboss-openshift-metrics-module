Name:          jboss-openshift-metrics-module
Version:       1.0.0
Release:       1%{?dist}
Summary:       Collects and exposes runtime metrics from a running JBoss instance.
License:       ASL 2.0
URL:           http://www.openshift.com
Source0:       jboss-openshift-metrics-module-%{version}.tar.gz
BuildRequires: java
BuildRequires: maven3
BuildArch:     noarch

%global as7_module_dir /etc/alternatives/jbossas-7/modules/
%global eap_module_dir /etc/alternatives/jbosseap-6/modules/
%global metrics_module_dir com/openshift/metrics/

%description

%prep
%setup -q

%build
mvn clean package

%install
%__mkdir -p %{buildroot}%{as7_module_dir}
%__mkdir -p %{buildroot}%{eap_module_dir}

%__cp -r target/module/* %{buildroot}%{as7_module_dir}
%__cp -r target/module/* %{buildroot}%{eap_module_dir}

%__cp quartz.jar %{buildroot}%{as7_module_dir}%{metrics_module_dir}main
%__cp quartz.jar %{buildroot}%{eap_module_dir}%{metrics_module_dir}main

%files
%defattr(-,root,root,-)
%{as7_module_dir}%{metrics_module_dir}
%{eap_module_dir}%{metrics_module_dir}

%changelog
