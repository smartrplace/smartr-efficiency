# SmartrEfficiency
Platform for planning building energy efficiency and upgrade projects and for managing building-related data

## Overview
Smartrplace plans to offer the SmartrEfficiency platform as a free Internet service in the near future. Users can enter data regarding there buildings and can perform several evaluations ("calculators") on this data. These calculators generate project proposals for energy efficiency projects and it shall also be possible to obtain offers including prices for some types of projects.<br>
The data management and calculators can also be installed and used offline on a local PC or other machine providing a JVM. The bundles provided in this repository can be used to start the offline version of the SmartrEfficiency platform. It will be possible to download personal and public data from the Internet platform and import it into the offline version so that all personal data can be used independently from the Internet service.<br>
Via the SmartrEfficiency API it is also possible to define new data types and calculators that can be developed and tested with the offline version and that can be added to the Internet platform as well via a simple code review process.

## Build
If you have installed the [OGEMA Software Development Kit (SDK)](https://community.ogema-source.net/xwiki/bin/view/Main/)
and you have run the standard rundir you should be able to build the entire repository. For further steps see [ogema-backup-parser#Build](https://github.com/smartrplace/ogema-backup-parser#build).

## Usage
An osgi-run-config will be provided soon. Note that bundles that shall be added to the internet platform need to run with activated OGEMA security and shall not change the standard permissions.perm file. So if you plan to publish your bundle for the platform test it with security enabled.

## API overview
All bundles providing extension to the platform must provide an OSGi service of type [ExtensionService](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/ExtensionService.java). There are two examples for this: [SmartrHeatingExtension](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/org/sp/example/smartrheating/SmartrHeatingExtension.java) and the more complex [BaseService](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-efficiency-util/src/main/java/org/smartrplace/smarteff/defaultservice/BaseDataService.java). Each service can register an arbitrary number of resource types and capabilities where three types of capabilities.<br>
Resource Types are registered with a type declaration as defined in the interface [ExtensionResourceTypeDeclaration](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/ExtensionResourceTypeDeclaration.java). The data types that are managed by users on SmartrEfficiency are only used at well defined positions in the resource tree. For this reason each such resource type defined a parent type. Resources of this type only will be created as children of the parent type or as elements of a resource list if multiple element can be attached to the parent. Toplevel resource types are buildings (BuildingData) and global parameter models.<br>
Currently there are three main types of capabilities:
* Pages that allow to create and edit new resources ("EditPage"). Such pages are usually inherited from the class EditPageGeneric or one of the derivated provided in [package editgeneric](https://github.com/smartrplace/smartr-efficiency/tree/master/smartr-efficiency-util/src/main/java/org/smartrplace/smarteff/util/editgeneric). Examples are provided:
    - Base version:[BuildingEditPage](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-efficiency-util/src/main/java/org/smartrplace/commontypes/BuildingEditPage.java)
    - Automatically also create table page for all sub resources of the type. This is especially relevant for resource types with a Multi-Cardinalty declared meaning more than one resource of this type can be added to a parent resource: [HeatBillRegistration.EditPage](https://github.com/smartrplace/smartr-efficiency/blob/a9f0d7d354fb028b516cfc1c44846515614bad79/smartr-efficiency-util/src/main/java/org/smartrplace/commontypes/HeatBillRegistration.java#L68). Here the respective resource type declaration is also part of the same Java file.
    - Files for editing parameter resources: For each public parameter resource a global instance is created that is used by default. Users can also set own values for some or all values defined by a parameter resource, so for editing users see both the global and the personal setting and can switch which value to use. See [DefaultProviderParamsPage](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-efficiency-util/src/main/java/extensionmodel/smarteff/defaultproposal/DefaultProviderParamsPage.java) for an example that provides parameters for a simplem default building evaluation.
* Pages that provide an overview table on several resources ("TablePage"): They usually extend the class ResourceTableProvider, which is given in [package defaultservice](https://github.com/smartrplace/smartr-efficiency/tree/master/smartr-efficiency-util/src/main/java/org/smartrplace/smarteff/defaultservice) with some examples. There are generic pages for most purposes already so you usually do not have to develop own TablePages or create them together with an EditPage via EditPageGenericWithTable as in the example above.
* Calculators (LogicProviders) that create result project proposals of type [ProjectProposal](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/proposal/ProjectProposal.java) or more general [CalculatedData](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/proposal/CalculatedData.java). Calculators also need parameters as input (see example of parameter EditPage above). ProjectProviders usually extends the class ProjectProviderBase or more general LogicProviderBase. The simple basic building calculator is given in [BuildingExampleAnalysis](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-efficiency-util/src/main/java/extensionmodel/smarteff/defaultproposal/BuildingExampleAnalysis.java) as example. For LogicProviders that use time series as input also LogicEvalProviderBase is available as template.
For each resource the LogicProviders onto which the resource can be applied are shown in the LogicProvTablePage. All For ProjectProposals a standard overview page exists (ResultTablePage).

## How and why are SmartrEfficiency extension bundles different from OGEMA bundles?
SmartrEfficiency do not offer a service of type Application as OGEMA bundles but the ExtensionService as explained above. On the Internet platform it has to be ensured that each user accesses only the personal data and the public global data, but not the data of other users. To allow for a very simple code review process of new components regarding this requirement a full access to the OGEMA ApplicationManager cannot be granted to extension bundles on the Internet platform - instead you get an adapted [ApplicationManagerSPExt](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/ApplicationManagerSPExt.java). On your offline instance of SmartrEfficiency you are free to add your own standard OGEMA bundles, of course.<br>
The ExtensionService interface is also very much focused on the SmartrEfficiency platform which makes the implementation of the specific components described above much simpler compared to an implementation based on the standard OGEMA API.<br>
As described above each resource type is only used at well defined positions in the resource tree. This allows to save a lot of resource listeners for the platform and searching for resources of a certain type when opening user interface pages which increases performance on a large OGEMA database. This implies that the extension bundles cannot register normal OGEMA resource demands. This is sufficient for such planning and data management tasks but would not fit the requirements of an OGEMA automation platform like the SmartrplaceBox for heat and energy control in the building. Here bundles that use the normal OGEMA API can be added via the OGEMA Appstore.

##Timeseries
Time series can be represented directly in the OGEMA data base as RecordedData or Schedules. But time series can also be provided by file uploads or external data bases that are no imported persistently into the OGEMA data base. A generic data model that can be used to reference any of these time series input formats is [SmartEffTimeSeries] (https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-efficiency-api/src/main/java/extensionmodel/smarteff/api/base/SmartEffTimeSeries.java). This model is mostly treated as other ValueResources by the framework (whereas Schedules and RecordedData can only exist as sub resources of certain SingleValueResources).
Applications can create time series information via [ExtensionPageSystemAccessForTimeseries] (https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/resourcecreate/ExtensionPageSystemAccessForTimeseries.java). In this way the time series will be accessible for EvaluationProviders via the standard DataProvider GenericDriverProvider. The respective GaRoMultiEvalDataProvider provides
all time series below a building or a room recursively with fitting GaRo type requested by an evaluation. 
Information on timeseries uploaded is stored in the top-level resource genericTSDPConfig  of Type GenericTSDPConfiG. 

## License
[GPL v3](https://www.gnu.org/licenses/gpl-3.0.en.html)