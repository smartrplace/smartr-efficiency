# Spreadsheet2Portal - Converting spreadsheets into SmartrEfficiency calculators

## Introduction
Spreadsheet application software like Microsoft Excel and OpenOffice/LibreOffice Calc make it possible to develop simple calculation tools very quickly, including data acquisition, presentation and monitoring of intermediate results. These programs are not very suitable, though, when data from various users and buildings needs to be collected and held in various user accounts via a web tool allowing different users to calculate individual results via the internet. If users would use the spreadsheet calculations directly they would have to copy the data manually if switching to a new version of the spreadsheet, most likely users would not update to a new version. Users also would have to install the right spreadsheet calculation program and providing an optimal data experience is also limited when the input data is gaining complexity. It would also not be possible to perform anonymous statistical analysis over the data of all users who agree to be part of such an evaluation. For this reason the Spreadsheet2Portal functionality provides a spreadsheet structure, documentation and code that allows a quick setup of a building or district-related calculator in Calc or Excel and also allows for a quick conversion into the SmartrEfficiency modules required to provide the calculator as a web tool inside a SmartrEfficiency portal.

## Spreadsheet structure
Note: The structure information given here is used by the MultiBuilding example. The initial HPAdapt example is currently not fully compatible with this structure yet. This structure can be used with the [SmartEffRes helper library](https://gitlab.com/jakobbbb/hpadapt-draftcalc/blob/master/lib/README.md), which allows you to access resources in a spreadsheet by name rather than by cell coordinates.

### Worksheets
The spreadsheet provides the different input types for a [LogicProvider](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-efficiency-util/src/main/java/org/smartrplace/smarteff/util/LogicProviderBase.java) in different worksheets. The names of the worksheets can be given any name by the developer of the spreadsheet application in principle, but the following names are recommended that are also used in the examples:
* "param": Overall parameters that are not specific for users or buildings. If the calculator shall have internal parameters then a separate worksheet "InternalParams" shall be used for this.
* "data": The data of the building or other user specific data set that shall be calculated. If data from more than one building is to be held in the spreadsheet file then for each building a separate worksheet shall be created with the same structure. The data that shall be used for processing is then copied manually into the "LastBuilding" worksheet before the calculation is started.<br>
Usually also the core results are stored here. For 100EE-refurbishment projects this are usually the results defined in the interface [ProjectProposal100EE](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/proposal/ProjectProposal100EE.java) including [ProjectProposal](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/proposal/ProjectProposal.java).
* "lastCalc": Intermediate results of the last calculation are stored and presented here
* Separate spreadsheets can be added to provide time series and other larger data sets used as input

### Data structure
The data structure within in each worksheet is documented in [SmartEffRes helper library](https://gitlab.com/jakobbbb/hpadapt-draftcalc/blob/master/lib/README.md#table-format)

Early proposal for further discussion: Data elements that are already available as resources in other models shall be marked green. If the designer does not
know whether resource elements already exist they can be marked yellow (see next Section). Data elements that shall be represented as resources in the portal shall be marked yellow. For parameters and LastBuilding data all input that is required for the calculation shall be marked yellow as all input from users needs
to be represented in resources. Elements of LastCalculation may just be represented in the calculator internally and not mode visible in the portal if not marked yellow. Data elements that are results shall be marked blue if they shall be represented in the result resource model, not in the resource model generated from LastBuilding, although they are part of the LastBuidling worksheet usually.<br>

### Element documentation
The documentation of each data element in the different worksheets and the respective OGEMA data models is decisive for the development and re-usability of the components. The human-readable information in the **Name** column typically should provide a definition that allows to understand the meaning of the value. Typically the same information should be provided with the respective OGEMA resource definition, a bit more extended in some cases. This typically also is close to the text shown in the respective data entry mask in online portal (with translations and sometimes more simple language to be suitable for end users). In most cases a more extensive documentation describing the exact semantics of each value is still required. If this is placed in the OGEMA resource definition this information is not accessible in the online portal and Javadoc is also limited to embed this in a nicely formatted description of the calculator together with its parameters. So this usually should be made in a Wiki and/or a markdown file. Links to the markdown file shall be provided in the online portal and could also be added to the respective OGEMA Javadoc elements, but this is not required. In the class documentation a link may be added to the EditPage where the links to the detailed documentation are defined, e.g. " For links to detailed documentation see {@link HPAdaptEditPage}". The file [HPAdapt.md](https://github.com/smartrplace/smartr-efficiency/blob/master/HPAdapt.md) provides a sample documentation.<br>
Note: Elements that do not require additional documentation need no entry in the markdown page and do not need to be linked. It is also possible to summarize several elements into a common link.

### VBA code
The calculator can use spreadsheet formulas and VBA code, which can be executed both in Microsoft Excel and Calc. It is important to understand that when a cell is written via VBA in a macro all cells depending on the cell written via spreadsheet formulas are updated before the script execution is further processed. This is great for development, but can cost a lot of performance if these calculations are triggered e.g. in a loop.

## Development of the online calculator in the SmartrEfficiency portal
The development of the calculator usually comprises several steps:
* The respective resources have to be defined based on the information from the spreadsheet. If the column with the resource names is not yet filled and English translations are missing this may have to be added at this point. Also the correct resource types have to be selected here. See the [HPAdapt data models](https://github.com/smartrplace/smartr-efficiency/tree/master/smartr-heating-server/src/main/java/extensionmodel/smarteff/hpadapt) for an example.
* Pages for data entry for the data models have to be defined. See [HPAdaptEditPage](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/org/sp/calculator/hpadapt/HPAdaptEditPage.java), [HPAdaptParamsPage](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/org/sp/calculator/hpadapt/HPAdaptParamsPage.java) and [HPAdaptResultPage](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/org/sp/calculator/hpadapt/HPAdaptResultPage.java) as examples.
* The calculator has to be developed representing VBA code and spreadsheet formulas. See [HPAdaptEval](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/org/sp/calculator/hpadapt/HPAdaptEval.java) as an example. Some recommendations for this:
** Variable names should equal the respective resource name or the respective VBA variable.
** The structure of SUB functions in VBA shall be maintained
* Initialize parameters with the values provided in the spreadsheet document.

Note: The example spreadsheets HPAdapt.ods and HPAdapt.xlsm will be provided soon.

# Modeling
The development of the data models as parameters, individual data and results is a decisive step for the development of calculator modules. In principle similar challenges arise as for the modelling of normal OGEMA resources, a process for which some information is collected by the [OGEMA Alliance on the modeling process page](https://community.ogema-source.net/xwiki/bin/view/OGEMA%20Alliance%20Pages/Model%20Extension%20Process/).
A summary of the SmartrEfficiency modeling concept:
* The parameter models are not linked to any resource such as a building etc outside each single parameter table/resource. The meaning of each parameter value must be understandable via the documentation and via the internal structure of the single parameter model. For this reason parameters that are defined in another calculator can be accessed by copying the parameter sheet from the respective calculator or by a cross-document access (to be developed)
* The specific data models are linked to a major resource type such as BuildingData. The parent type shall be documented below each table together with CSVImportExportStructure and CSVImportExportTop via the line CSVImportExportParent. In order to use data from another calculator it must be made sure that the data belongs to the same parent resorce, e.g. the same building. Substructures like apartments, rooms and devices shall use common subtable names. The first room structure is defined in HPAdapt.
* For projects a separate start page like the building page is required (TODO). Also a structure for PersonalData comprising information attached to a person or the user logged in will most likely be required in the future.
* Wheres downward compatibility of new models is an important demand on the OGEMA modeling process this is not always the case for SmartrEfficiency models. It is assumed that all calculators for a certain portal are managed in a way that corrections to an existing model can be applied to all calculators using it.
* Each value shall only be modeled once and cross-calculator model usage and access shall be done whenever the same data is used in different calculators. If this was not identified in the first place model corrections with appropriate documentation shall take place.
* Dependencies between calculators may occur not only due to a common usage of values but also due to a common usage of logic - e.g. one calculator may use another one. In this case the logic may be used via calling the calculation method as a util or via providing the full input data model for the calculator used. When more complex calculators are used usually the latter approach has to be implemented.
  
# Create or update portal calculator based on Spreadsheet
* Add all relevant data elements from parameters, data and lastCalc to the calculator models. For each data type perform the following steps:
** Add data elements to the OGEMA resource type
** Add entries into the respective EditPage
** Add initialization: In calculator for parameters, in EditPage#defaultValues() for data
* Add the documentation markdown file and set link to WIKI in the calculator WIKI_LINK constant.
* Implement first subcalculators, then lastCalc sheet, the standard results that are usually at the bottom of the data sheet:
** Note that parameters of sub calculators are not doubled as they are global, but data sheets of sub-calculators
** Implement calculation backward: Start to with setting the results and collect the required data according to the spreadsheet until you have calculated everything based on the input data.
  
## Open Issues
 * Comment column in Spreadsheet (currently link column is used)
 * 