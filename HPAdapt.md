# Bivalent heat pump/boiler operation with fully adaptive supply temperature

## Introduction
The APAdapt calculator is designed to generate proposals for the heat
supply of existing building when a gas boiler needs replacement that
currently takes over the full heat supply of the building. The focus of
the concept calculated is the implementation of a heat pump while
maintaining existing heat radiators in the building combined with a
highly adaptive supply temperature of the heat pump.  [...]

##### Table of Contents
[Introduction](#introduction)  
[Parameters](#parameters)  
[Data](#data)  
[Results](#results)  
[Software background](#software-background)  

## Parameters
[`HPAdaptParams`](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/extensionmodel/smarteff/hpadapt/HPAdaptParams.java)  
<!--
### Price of CO₂-neutral electricity (EUR/kWh)
`pricesCO2neutral.electrictiyPricePerkWh`  

### Price of 100EE electricity (EUR/kWh)
`prices100EE.electrictiyPricePerkWh`  

### Price of heat pump electricity (EUR/kWh)
`pricesConventional.electrictiyPriceHeatPerkWh`  

### Price of CO₂-neutral heat pump electricity (EUR/kWh)
`pricesCO2neutral.electrictiyPriceHeatPerkWh`  

### Price of 100EE heat pump electricity (EUR/kWh)
`prices100EE.electrictiyPriceHeatPerkWh`  

### Price of conventional natural gas (EUR/kWh)
`pricesConventional.gasPricePerkWh`  

### Price of CO₂-neutral gas (EUR/kWh)
`pricesCO2neutral.gasPricePerkWh`  

### Price of 100EE gas (EUR/kWh)
`prices100EE.gasPricePerkWh`  

### Condensing Boiler → Condensing Boiler (CD→CD), base price (EUR)
`boilerChangeCDtoCD`  

### Low-Temperature Boiler → Condensing Boiler (LT→CD), base price (EUR)
`boilerChangeLTtoCD`  

### Additional CD→CD (EUR/kW)
`boilerChangeCDtoCDAdditionalPerkW`  

### Additional LT→CD (EUR/kW)
`boilerChangeLTtoCDAdditionalPerkW`  

### Additional Base Cost of Bivalent Heat Pump (EUR)
`additionalBivalentHPBase`  

### Additional Base Cost of Bivalent Heat Pump (EUR/kW)
`additionalBivalentHPPerkW`  

### Boiler Power Reduction switching from LT→CD
`boilerPowerReductionLTtoCD`  

-->

### Historical Temperature Data to be imported via CSV
`temperatureHistory`  

One year of daily mean outside temperatures.  You can use the default
value and set an
[offset](#offset-for-adapting-to-historical-outside-temperature-data-k)
or upload your own data in the following format:
```
Tag;Mitteltemperatur
01.01.2018 00:00;-3,512
02.01.2018 00:00;-4,728
[...]
31.12.2018 00:00;-5,952
```

## Data
[`HPAdaptData`](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/extensionmodel/smarteff/hpadapt/HPAdaptData.java)  

### Default room height in building
`roomHeight`  
Can be overridden by heights set for individual rooms.  If a room has no
individual height defined, this height will be used.

### Estimated savings after basic renovation
`savingsAfterBasicRenovation`  
When a new heat supply system is introduced into the building this is a
very good time to improve the building insulation at the same time
allowing to reduce the installed power of the supply system. Even if a
full refurbishment of the building envelope is not feasible at this
point at least the most economical improvements should be considered.
This is the percentage of energy consumed before the refurbishment
estimated to be saved by improvements of the building envelope.

### Known or estimated warm drinking water consumption
`wwConsumption`  
Warm drinking water consumption per year in m³.


<!--
### Warm water temperature
`wwTemp`  

### Warm water temperature can be lowered to
`wwTempMin`  
-->
### Heating limit temperature
`heatingLimitTemp`  
Outside temperature below which heating is required.
<!--
### Outside design temperature
`outsideDesignTemp`  

### Estimated savings from condensing boiler
`savingsFromCDBoiler`  

### Dimensioning for price type
`dimensioningForPriceType`  

### U-Value basement in relation to U-Value facade (equal = 1.0)
`uValueBasementFacade`  

### U-Value roof in relation to U-Value facade (equal = 1.0)
`uValueRoofFacade`  

### Thickness of inner walls
`innerWallThickness`  

### Basement temperature during heating season
`basementTempHeatingSeason`  
-->
### Offset for adapting to historical outside temperature data (K)
`outsideTempOffset`  
Offset that can optionally be applied to
[historical temperature data](#historical-temperature-data-to-be-imported-via-csv).

## Results
[`HPAdaptResult`](https://github.com/smartrplace/smartr-efficiency/blob/master/smartr-heating-server/src/main/java/extensionmodel/smarteff/hpadapt/HPAdaptResult.java)  

<!--
### Boiler power (boiler only)
`boilerPowerBoilerOnly`  

### Boiler power (bivalent heat pump)
`boilerPowerBivalentHP`  

### Heat pump power (bivalent heat pump)
`hpPowerBivalentHP`  

### Warm water energy (pre-renovation) (kWh)
`wwEnergyPreRenovation`  

### Heating energy (pre-renovation) (kWh)
`heatingEnergyPreRenovation`  

### Warm water energy (post-renovation) (kWh)
`wwEnergyPostRenovation`  

### Heating energy (post-renovation) (kWh)
`heatingEnergyPostRenovation`  

### Total energy (post-renovation) (kWh)
`totalEnergyPostRenovation`  

### Heating degree days
`heatingDegreeDays`  

### Number of heating days
`numberOfHeatingDays`  

### Heating degree days (hourly basis)
`heatingDegreeDaysHourly`  

### Number of heating days (hourly basis)
`numberOfHeatingDaysHourly`  

### Full load hours excl. warm water (h/a)
`fullLoadHoursExclWW`  

### Full load hours incl. warm water (h/a)
`fullLoadHoursInclWW`  

### Mean heating outside temperature
`meanHeatingOutsideTemp`  

### Maximum power of heat pump from BadRoom
`maxPowerHPfromBadRoom`  

### Window area
`windowArea`  

### Window power loss (W/K)
`pLossWindow`  

### Number of rooms facing outside
`numberOfRoomsFacingOutside`  

### Facade wall area
`facadeWallArea`  

### Basement area
`basementArea`  

### Roof area
`roofArea`  

### Weighted exterior surface area excl. windows
`weightedExtSurfaceAreaExclWindows`  

### Active power while heating
`activePowerWhileHeating`  

### Total power loss (W/K)
`totalPowerLoss`  

### U-Value of facade
`uValueFacade`  

### Basement heating power loss
`powerLossBasementHeating`  

### Other power loss (W/K)
`otherPowerLoss`  

### Power loss at 0°C
`powerLossAtFreezing`  
-->

## Software background
The calculator uses the [Spreadsheet2Portal](https://github.com/smartrplace/smartr-efficiency/blob/master/Spreadsheet2Portal.md) concept.
Note: The example spreadsheets HPAdapt.ods and HPAdapt.xlsm will be provided soon.
