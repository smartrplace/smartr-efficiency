# MultiBuilding energy solutions calculator

## Introduction
The MultiBuilding calculator is designed to provide cost calculations for IoT projects for buildings. The calculator is designed for IoT solutions that are deployed to more than one building meaning that each hardware and software setup can be used in an arbitrary number of buildings. The calculator can also be used for solutions that are only placed in a single building.<br>
Derived calculators shall also estimate benefits from certain applications for owners, tenants, energy efficiency etc.<br>
Note that in the first version only a single hardware setup is supported that is used in all buildings within a single project.


##### Table of Contents
[Introduction](#introduction)  
[Parameters](#parameters)  
[Data](#data)  
[Results](#results)  
[Software background](#software-background)  

## Parameters
[`HPAdaptParams`](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/extensionmodel/smarteff/multibuild/MultiBuildParams.java) 

### Cost of SmartrplaceBox per Building (EUR)
`costSPBox`  
Base cost of the SmartrplaceBox excluding extensions for special communication protocols. The standard SmartrplaceBox is delivered in a round, Smart Home-featured casing. 

### Project base cost
`costProjectBase`  
Base cost for each individual project that covers acquisition and further base cost. A more detailed and flexible calculation shall be set up in the future. In this version this covers at least two travellings to the customer site within Germany (for project preparation and for installation).

### Operational cost per building/year
`operationalCost`  
Operational cost for the base system per year. This includes a contribution to the central server fee and base support.

### Hardware components for buildings
`buildingComponents`  
This ResourceList contains hardware components suitable for deployment in buidlings that have been evaluated or tested for usage in IoT projects. The elements contain the following elements:

#### Cost
`cost`  
The initial cost for the customer including hardware cost, configuration and installation support. A more flexible model for the cost including deductions for larger quantities shall be developed in the future.

#### Yearly Cost
`yearlyCost`  
Components that require a cloud service or regular maintenance may have a direct yearly cost attached. Otherwise it is assumed that the lifetime of the hardware is sufficient for the project. A model suitable for modeling refurbishment and failure replacement cost may be added in the future. Also costs for energy consumption including battery replacement are not included yet.

#### Communication Bus or hardware interface
`comBus`  
This indicates the requirements of communication adapters that have to be provided once with the SmartrplaceBox or on the LAN. The String given here should be an element name of the list "Communication adapters for SmartrplaceBoxes". The cost of the respective adapter must be added to once for each communication bus at least by one sensor of the IoT system.

#### Link
`link`  
This may directly link to a documentation or shop page of the hardware component. It may also link to a wiki page describing the usage and linking to suppliers and further documentation.

### Hardware in Buildings (Cost per unit)
`insideTempCost`
`outsideTempCost`
`waterTempCost`
`powerSwitchCost`
`powerMeterCost`
These are hardware components like those in [Hardware components for buildings](MultiBuilding.md#hardware-components-for-buildings). The elements are kept for compatibility reasons.


## Data
[`MultiBuildingData`](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/extensionmodel/smarteff/multibuild/MultiBuildDat.java)  

### Number of Buildings
`buildingNum`  
Number of buildings using the hardware / software setup defined for each building.

### Operational cost overall/year
`operationalCost`  
Project-specific operational cost per year in EUR. These cost are added to the yearly cost determined via parameters.

### Hardware components for buildings selected
`buildingComponents`
The names in these tables must be names in the table [Hardware components for buildings](MultiBuilding.md#hardware-components-for-buildings). These lines indicate the hardware components and the quantities used per building in the IoT project.

### Hardware in Buildings
`insideTempNum`
`outsideTempNum`
`waterTempNum`
`powerSwitchNum`
`powerMeterNum`
Kept for compatibility reasons

## Results
[`MultiBuildResult`](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/extensionmodel/smarteff/multibuild/MultiBuildResult.java)  

### Cost per building
`costPerBuilding`  
Variable cost per building including hardware and configuration effort that applies for each building. This does not contain any overall project cost, such cost is NOT distributed on the buildings in the calculation of this value.


## Software background
The calculator uses the [Spreadsheet2Portal](https://github.com/smartrplace/smartr-efficiency/blob/master/Spreadsheet2Portal.md) concept.
Note: The example spreadsheets MultiBuilding.ods and MultiBuilding.xlsm will be provided soon.
