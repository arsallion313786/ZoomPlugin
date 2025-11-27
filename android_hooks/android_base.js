/** @module Android Hooks for ZoomVideo Cordova Plugin */
var path = require("path");
var fs = require("fs");
 
/**
* @desc Hooks to manage Gradle build script changes
* when ZoomVideo plugin is added or removed
*/
module.exports =  {
 
 
      
 
 
  /**
   * ZoomVideo build extension
   */
  // gradleScriptChanges: "\n      classpath 'com.appdynamics:appdynamics-gradle-plugin:24.4.2'",
 customScriptPath : path.join(context.opts.projectRoot, 'android_hooks', 'zoomvideo.gradle'),
 
  /**
   * Path for android gradle file - from Android 7.0
   */
  androidGradleScriptPath: path.join("platforms", "android", "app", "build.gradle"),
 
  /**
   * Path for android gradle file - before Android 7.0
   */
  androidGradleScriptPathBefore70: path.join("platforms", "android", "build.gradle"),
 
  /**
   * Get gradle script as string
   * @param path
   * @returns {String} - Gradle script path as a string
   */
  readFile: function (path) {
    return fs.readFileSync(path, "utf-8");
  },
 
  /**
   * Write contents to a path
   * @param path {String}
   * @param contents {String}
   */
  writeFile: function (path, contents) {
    fs.writeFileSync(path, contents);
  },
 
  /**
   * Helper method to read the Android gradle file.
   * @returns {String} Android Gradle File
   */
  readAndroidGradle: function () {
     if (fs.existsSync(this.androidGradleScriptPath)) {
        return this.readFile(this.androidGradleScriptPath);
     } else {
        return this.readFile(this.androidGradleScriptPathBefore70);
     }
  },
 
  /**
   * Write contents to the Android gradle build path
   * @param {String} contents - The contents to be written to the gradle file.
   */
  writeAndroidGradle: function (contents) {
    if (fs.existsSync(this.androidGradleScriptPath)) {
       this.writeFile(this.androidGradleScriptPath, contents);
    } else {
       this.writeFile(this.androidGradleScriptPathBefore70, contents);
    }
  },
 
  /**
   * Delete all references to ZoomVideo agent from build script.
   */
  deleteAgentFromProjectGradle: function () {
    var buildScript = this.readAndroidGradle();
    var regex = new RegExp(this.gradleScriptChanges);
    this.writeAndroidGradle(buildScript.replace(regex, ""));
  },
 
  /**
   * Inject the zoomvideo Gradle Plugin in Android gradle build script
   */
  injectAgentPlugin: function (context) {
    var buildGradle = this.readAndroidGradle();
    buildGradle = buildGradle.replace(/(.*com.android.tools.build:gradle.*)/, '$1' + this.gradleScriptChanges);
    this.writeAndroidGradle(buildGradle);
  },

  injectZoomVideoGradle: function (context) {

    
    

    if (fs.existsSync(this.androidGradleScriptPath) && fs.existsSync(this.customScriptPath)) {
        let buildGradle = fs.readFileSync(this.androidGradleScriptPath, 'utf8');
        const customScript = fs.readFileSync(this.customScriptPath, 'utf8');

        if (!buildGradle.includes(customScript)) {
            buildGradle += `\n${customScript}`;
            this.writeAndroidGradle(buildGradle);
            //fs.writeFileSync(gradleFilePath, buildGradle, 'utf8');
            console.log('Custom script added to build.gradle');
        }
    } else {
        console.error('build.gradle or customScript.gradle not found');
    }
  },
 
  /**
   * Handle deprecated android.registerTransform
   * This method checks if the deprecated API is being used and replaces it
   */
  handleDeprecatedTransform: function() {
    var buildGradle = this.readAndroidGradle();
    // Check if android.registerTransform exists and remove/replace it
    if (buildGradle.includes('android.registerTransform')) {
      console.log('Found deprecated android.registerTransform, removing...');
      buildGradle = buildGradle.replace(/android\.registerTransform/g, '');
      this.writeAndroidGradle(buildGradle);
      console.log('Removed deprecated android.registerTransform');
    }
  }
};