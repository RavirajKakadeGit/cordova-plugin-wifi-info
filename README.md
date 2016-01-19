# NetworkManager

Version 0.1.0

## Using
Clone the plugin

    $ git clone https://github.com/RavirajKakadeGit/cordova-plugin-wifi-info.git

Create a new Cordova Project

    $ cordova create hello com.example.hello HelloWorld

Install the plugin

    $ cd hello
    $ cordova plugin add ../cordova-plugin-wifi-info

### Usage from within Cordova/Phonegap:

#### `NetworkManager.getAvailableWifiList(win,fail);`

Retrieves a list of the available networks as an array of objects:

    [
        {   "SSID": ssid,
            "BSSID": bssid,
            "frequency": frequency,
            "capabilities": capabilities
        }
    ]

#### `NetworkManager.getConnectionInfo(win,fail);`

Retrieves connected wifi information :


#### `NetworkManager.addWifiNetwork(wifi, win, fail);`

Add the network to the lists

`wifi` needs to be an object Currently, WPA,WPA2,WEP,ESS is supported for the algorithm.

    var wifi = {
        SSID : "demo" ,
        algorithm : "WPA" ,
        pass : "****"
     }

`win` and `fail` are callback functions

