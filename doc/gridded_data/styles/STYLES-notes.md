##ColorRamp Plugin

`http://docs.geoserver.org/stable/en/user/community/colormap/index.html#installing-the-dynamic-colormap-community-extension`
+ Pom.xml config:
   ```
          <dependency>
                 <groupId>org.geoserver.community</groupId>
                 <version>${geoserver.version}</version>
                 <scope>runtime</scope>
                 <artifactId>gs-colormap</artifactId>
             </dependency>
             ```

+ Applies a SVG style rule file. 
 + GetMap requests can therefore request different palletes `...&env=pallette:GMT_seafloor;`
 
+ Plugin looks for the SVG files in `styles/ramps` Sub folders can be used with a backslash notation
+  Page of links to SVG style files: `http://soliton.vm.bytemark.co.uk/pub/cpt-city/`
 + Suitable files: `http://soliton.vm.bytemark.co.uk/pub/cpt-city/gmt/index.html`. 
  + All these files need stripping, leaving only the `linearGradient` tag
  + They also need no spaces. `stop-color="rgb(105, 0, 182)"` causes an error

+ There is a maximum amount of rules (255) in a file that can be used. It seems a rule in a svg file is equivalent to SLD ColorMapEntry in a RasterSymbalizer tag.
+ GetLegendGraphic requests dont work correctly.

#### Dynamic data
1. The min and max values can be read in dynamically from the data   ***todo yet***

