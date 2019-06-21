# Spreadsheet2Portal - Converting spreadsheets into SmartrEfficiency calculators

## Introduction
Spreadsheet application software like Microsoft Excel and OpenOffice/LibreOffice Calc make it possible to develop simple calculation tools very quickly, including data acquisition, presentation and monitoring of intermediate results. These programs are not very suitable, though, when data from various users and buildings needs to be collected and held in various user accounts via a web tool allowing different users to calculate individual results via the internet. If users would use the spreadsheet calculations directly they would have to copy the data manually if switching to a new version of the spreadsheet, most likely users would not update to a new version. Users also would have to install the right spreadsheet calculation program and providing an optimal data experience is also limited when the input data is gaining complexity. It would also not be possible to perform anonymous statistical analysis over the data of all users who agree to be part of such an evaluation. For this reason the Spreadsheet2Portal functionality provides a spreadsheet structure, documentation and code that allows a quick setup of a building or district-related calculator in Calc or Excel and also allows for a quick conversion into the SmartrEfficiency modules required to provide the calculator as a web tool inside a SmartrEfficiency portal.

## Spreadsheet structure
### Worksheets
The spreadsheet provides the different input types for a [LogicProvider](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-efficiency-util/src/main/java/org/smartrplace/smarteff/util/LogicProviderBase.java) in different worksheets:
* "Parameter": Overall parameters that are not specific for users or buildings. If the calculator shall have internal parameters then a separate worksheet "InternalParams" shall be used for this.
* "LastBuilding": The data of the building that shall be calculated. If data from more than one building is to be held in the spreadsheet file then for each building a separate worksheet shall be created with the same structure. The data that shall be used for processing is then copied manually into the "LastBuilding" worksheet before the calculation is started.<br>
Usually also the core results are stored here. For 100EE-refurbishment projects this are usually the results defined in the interface [ProjectProposal100EE](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/proposal/ProjectProposal100EE.java) including [ProjectProposal](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-domain-extension-api/src/main/java/org/smartrplace/extensionservice/proposal/ProjectProposal.java).
* "LastCalculation": Intermediate results of the last calculation are stored and presented here
* Separate spreadsheets can be added to provide time series and other larger data sets used as input

### Data structure
It is recommended to reserve the first two columns for version control (columns A,B). The third column (C) usually contains the value description in the language used for design. For scalar values (single values) the forth column (D) shall contain the actual value, the fifth column (E) contains the physical unit, the sixth column (F) contains the English translation, which can be omitted if the calculator is only developed in English language at this point. Column G contains the resource name to be used to store the value in the portal.<br>
For more complex data containing more than one value a suitable structure shall be selected. Usually a similar structure can be given in a header line to more a set of rows with more complex data.<br>
Data elements that are already available as resources in other models shall be marked green. If the designer does not
know whether resource elements already exist they can be marked yellow (see next Section). Data elements that shall be represented as resources in the portal shall be marked yellow. For parameters and LastBuilding data all input that is required for the calculation shall be marked yellow as all input from users needs
to be represented in resources. Elements of LastCalculation may just be represented in the calculator internally and not mode visible in the portal if not marked yellow. Data elements that are results shall be marked blue if they shall be represented in the result resource model, not in the resource model generated from LastBuilding, although they are part of the LastBuidling worksheet usually.<br>
In VBA make sure that lines are referenced relative to the heading of each section the heading line number
being given by a constant so that the macro can be adapted easily if new lines are necessary. Inside each section
new lines should only be added to the end so that the line number relative to the section heading does
not change.<br>
Lists should be placed below the single-value scalar elements if possible in each worksheet so that the line numbers
of single value elements do not change when new list elements are added. In the VBA macro code the header of
each list has to be searched to find the starting line of the respective list. If list elements shall
be referenced as another value the index of the list element is used in VBA. In the OGEMA resources
usually just a reference to the respective element is made, so the index value is not relevant for
the SmartrEfficiency calculator development and can be omitted from the model.

### VBA code
The calculator can use spreadsheet formulas and VBA code, which can be executed both in Microsoft Excel and Calc. It is important to understand that when a cell is written via VBA in a macro all cells depending on the cell written via spreadsheet formulas are updated before the script execution is further processed. This is great for development, but can cost a lot of performance if these calculations are triggered e.g. in a loop.

## Development of the calculator
The development of the calculator usually comprises several steps:
* The respective resources have to be defined based on the information from the spreadsheet. If the column with the resource names is not yet filled and English translations are missing this may have to be added at this point. Also the correct resource types have to be selected here. See the [HPAdapt data models](https://github.com/smartrplace/smartr-efficiency/tree/master/smartr-heating-server/src/main/java/extensionmodel/smarteff/hpadapt) for an example.
* Pages for data entry for the data models have to be defined. See [HPAdaptEditPage](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/org/sp/calculator/hpadapt/HPAdaptEditPage.java), [HPAdaptParamsPage](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/org/sp/calculator/hpadapt/HPAdaptParamsPage.java) and [HPAdaptResultPage](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/org/sp/calculator/hpadapt/HPAdaptResultPage.java) as examples.
* The calculator has to be developed representing VBA code and spreadsheet formulas. See [HPAdaptEval](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/org/sp/calculator/hpadapt/HPAdaptEval.java) as an example. Some recommendations for this:
** Variable names should equal the respective resource name or the respective VBA variable.
** The structure of SUB functions in VBA shall be maintained
* Initialize parameters with the values provided in the spreadsheet document.

Note: The example spreadsheets HPAdapt.ods and HPAdapt.xlsm will be provided soon.