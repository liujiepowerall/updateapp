var updateAppPlugin =
    { createEvent: function(successCallback, errorCallback,checkPath)
        {
            cordova.exec(
                successCallback, // success callback function
                errorCallback, // error callback function
                'UpdateAppPlugins', // mapped to our native Java class called "CalendarPlugin"
                'checkAndUpdate', // with this action name
                [checkPath]
            );
        }
    };