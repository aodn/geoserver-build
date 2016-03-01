<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
   xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
   xmlns="http://www.opengis.net/sld"
   xmlns:ogc="http://www.opengis.net/ogc"
   xmlns:xlink="http://www.w3.org/1999/xlink"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <NamedLayer>
   <Name>currents</Name>
   <UserStyle>
     <Title>currents</Title>
     <FeatureTypeStyle>
        <Rule>
          <Name>rule1</Name>
          <Title>Opaque Raster</Title>
          <Abstract>A raster with 100% opacity</Abstract>
          <RasterSymbolizer>
            <ColorMap>
              <ColorMapEntry color="#0000ff" quantity="-32768" opacity="0"/>
              <ColorMapEntry color="#0000ff" quantity="268"/>
              <ColorMapEntry color="#00ffff" quantity="277"/>
              <ColorMapEntry color="#00ff00" quantity="286"/>
              <ColorMapEntry color="#ffff00" quantity="295"/>
              <ColorMapEntry color="#ff0000" quantity="304"/>
              <ColorMapEntry color="#ffff00" quantity="32768" opacity="0"/>
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
     </FeatureTypeStyle>
   </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>