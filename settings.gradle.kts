rootProject.name = "Lane"
include("LaneController")
include("LaneInstance")
include("LaneController:LaneControllerVelocity")
findProject(":LaneController:LaneControllerVelocity")?.name = "LaneControllerVelocity"
include("LaneInstance:LaneInstancePaper")
findProject(":LaneInstance:LaneInstancePaper")?.name = "LaneInstancePaper"
