// Raviraj Kakade

var NetworkManager = {
  /***
   * return available list of networks
   * @param win
   * @param fail
   */
    getAvailableWifiList : function(win,fail){
      cordova.exec(win, fail, 'NetworkManager', 'getWifiList', []);
    },

  /***
   * Get connection info
   */
  getConnectionInfo : function (win,fail) {
    cordova.exec(win,fail,'NetworkManager','getConnectionInfo',[]);
  },
  /***
   *
   * @param wifi - wifi object e.g var wifi = { SSID : "demo" , algorithm : "WPA" , pass : "****" }
   * @param win
   * @param fail
   */
    addWifiNetwork : function(wifi,win,fail){
    var nwObj = [];
      if(typeof wifi === "object"){
        console.log("NetworkManager - AddNetwork : Wifi is an Object");
        // Verify weather has SSID provide
        if(wifi.SSID !== undefined){
          nwObj.push(wifi.SSID);
        } else {
          console.log("NetworkManager - AddNetwork : SSID Required");
          return false;
        }

        if(wifi.algorithm !== undefined){
          nwObj.push(wifi.algorithm);
          nwObj.push(wifi.pass);
        } else {
          console.log("NetworkManager - AddNetwork : Algorithm Required");
          return false;
        }
        cordova.exec(win, fail, 'NetworkManager', 'configNewWifi', nwObj);
      } else {
        console.log("NetworkManager - AddNetwork : Wifi is not an Object");
      }
    }

};

module.exports = NetworkManager;
