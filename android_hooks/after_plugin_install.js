//var android = require("./android_base");

const fs = require('fs');
const path = require('path');
 
module.exports = function (context) {
  // Handle the deprecated android.registerTransform (if found)
 // android.handleDeprecatedTransform();
  // Continue with the existing methods
  //android.deleteAgentFromProjectGradle();
  //android.injectZoomVideoGradle(context);
  const gradleFilePath = path.join(context.opts.projectRoot, 'platforms', 'android', 'app', 'build.gradle');
  // const customScriptPath = path.join(context.opts.projectRoot,'plugins', 'cordova.plugin.zoomvideo', 'android_hooks', 'zoomvideo.gradle');

  if (fs.existsSync(gradleFilePath)) {
    //if(fs.existsSync(customScriptPath)){
      let buildGradle = fs.readFileSync(gradleFilePath, 'utf8');

      const linesToAdd = `
                minifyEnabled true
                shrinkResources true`;

        const linesToAdd2 = `debug {
                  minifyEnabled true
                  shrinkResources true
              }`;

              const debugLines = `
                minifyEnabled true
                shrinkResources true         
            `;     

              const linesToAdd3 = `ndk {
            abiFilters 'armeabi-v7a', 'arm64-v8a' // Include only the architectures you need
        }`;      

         const releaseExists = buildGradle.includes(linesToAdd);
        // const debugExists = buildGradle.includes('debug {');
         
        const buildTypesIndex = buildGradle.indexOf('buildTypes {');
        if(buildTypesIndex != -1){

          if (!releaseExists) {
            const releaseTypesIndex = buildGradle.indexOf('release {', buildTypesIndex);
            if (releaseTypesIndex !== -1) {
              const insertPosition = buildGradle.indexOf('}', releaseTypesIndex);
              if (insertPosition !== -1) {
                buildGradle = buildGradle.slice(0, insertPosition) + linesToAdd + '\n' + buildGradle.slice(insertPosition);
              }
            }
          }


          if(!buildGradle.includes('debug {')){


            const buildTypesIndex = buildGradle.indexOf('buildTypes {');

            if (buildTypesIndex !== -1) {
              const insertPosition = buildGradle.indexOf('\n', buildTypesIndex);
              if(insertPosition != -1){
                buildGradle = buildGradle.slice(0, insertPosition) + '\n'+ linesToAdd2 + '\n' + buildGradle.slice(insertPosition);
              }
            }
          }
          else{

            const debugLineTypesIndex = buildGradle.indexOf('debug {');
            if (debugLineTypesIndex !== -1) {
              const insertPosition = buildGradle.indexOf('\n', debugLineTypesIndex);
              if (insertPosition !== -1) {
                buildGradle = buildGradle.slice(0, insertPosition) + debugLines + buildGradle.slice(insertPosition);
              }
            }
          }


        }

        

        

        const ndkExists = buildGradle.includes(linesToAdd3);
        
        if (!ndkExists) {
          const defaultConfigTypesIndex = buildGradle.indexOf('defaultConfig {');
          if (defaultConfigTypesIndex !== -1) {
            const insertPosition = buildGradle.indexOf('\n', defaultConfigTypesIndex);
            buildGradle = buildGradle.slice(0, insertPosition) + '\n'  + linesToAdd3 + '\n' + buildGradle.slice(insertPosition);
            // const insertPosition = buildGradle.indexOf('}', defaultConfigTypesIndex);
            // if (insertPosition !== -1) {
            //   buildGradle = buildGradle.slice(0, insertPosition) + linesToAdd3 + '\n' + buildGradle.slice(insertPosition);
            // }
          }
        }
        

        fs.writeFileSync(gradleFilePath, buildGradle, 'utf8');







      // const customScript = fs.readFileSync(customScriptPath, 'utf8');

      // if (!buildGradle.includes(customScript)) {
      //     buildGradle += `\n${customScript}`;
      //     fs.writeFileSync(gradleFilePath, buildGradle, 'utf8');
      //     console.log('Custom script added to build.gradle');
      // }
    // }
    // else{
    //   console.error('customScript.gradle not found' + customScriptPath + "build.gradle path" + gradleFilePath);
    // }
      
  } else {
      console.error('build.gradle not found');
  }

};
