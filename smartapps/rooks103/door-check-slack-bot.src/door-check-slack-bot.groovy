/**
 *  SmartApp for being able to monitor any sensors with contact sensors. If they are open after 
 *  a certain time, we will start to send slack messages to the given channel informing people to
 *  actually close the doors, gradually escalating to @here messages or even @channel messages
 *
 *  Author: Matt Peterson
 */
 
include 'asynchttp_v1'
 
definition (
    name: "Door Check Slack Bot",
    namespace: "rooks103",
    author: "Matt Peterson",
    description: "SmartApp for sending Slack Notifications when a door is open beyond a certain time",
    category: "SmartThings Labs",
    iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics13-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics13-icn@2x.png"
)

preferences {
    page(name: "App Configuration", title: "Setup", install: true, uninstall: true) {
        section("Choose one or more, when...") {
            input "officeDoors", "capability.contactSensor", title: "Doors that will be monitored.", required: true, multiple: true
            input "startTime", "time", title: "Start sending messages", required: true
            input "hereTime", "time", title: "Switch to Here Messages", required: true
            input "channelTime", "time", title: "Switch to channel messages", required: true
        	input "endTime", "time", title: "Turn off messages", required: true
        }
        section("Slack Configuration") {
            input "slackURI", "text", title: "Slack Instance", required: true, description: "URI for Slack Instacne e.g. smartthings.slack.com"
            input "slackChannel", "text", title: "Slack Channel", required: true, description: "Channel to get message e.g. #general"
            input "slackToken", "password", title: "Slack API Token", required: true, description: "API Token for Slackbot"
        }
        section([mobileOnly:true]) {
            label title: "Assign a name", required: true
        }
    }
}

def installed() {   
    log.debug "It's installed."
    initialize()
}

def updated() {
    log.debug "It's updated."
    initialize()
}

def initialize() {
    schedule("0 0/20 * ? * MON-FRI", handlerMethod)
}

private Map paramsBuilder(msg) {
    Map slackParams = [
        uri: "https://$slackURI/api/chat.postMessage",
        headers: [
            "Authorization": "Bearer $slackToken"
        ],
        body: [
            channel: "$slackChannel",
            text: "$msg",
            icon_emoji: ":coffee:",
            as_user: true
        ]
    ]
    return slackParams
}

def handlerMethod() {
    def openDoors = officeDoors.findAll { doorVal ->
        doorVal.currentContact == "open" ? true : false
    }
    log.debug "${openDoors} -- ${openDoors.size()}"
    
    if (openDoors.size() != 0) {
        if (timeOfDayIsBetween(startTime, hereTime, new Date(), location.timeZone)) {
            log.debug "Normal message"
            asynchttp_v1.post(processResponse, paramsBuilder("Looks like some doors are open: ${openDoors}. We should probably close them now."))
        } else if (timeOfDayIsBetween(hereTime, channelTime, new Date(), location.timeZone)) {
            log.debug "Here message"
            asynchttp_v1.post(processResponse, paramsBuilder("<!here|here> These doors are open: ${openDoors}. If someone could please go close them, it would be much appreciated."))
        } else if (timeOfDayIsBetween(channelTime, endTime, new Date(), location.timeZone)) {
            log.debug "Channel message"
            asynchttp_v1.post(processResponse, paramsBuilder("<!channel|channel> For real. Someone go close these doors: ${openDoors}. Let's not invoke the wrath of Hann."))
        } else {
            log.debug "Shouldn't be here!"
        }
    } 
}

def processResponse(response, data) {
    
}