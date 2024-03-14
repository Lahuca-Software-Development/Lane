rootProject.name = "Lane"
include("LaneController")
include("LaneGame")
include("LaneController:LaneControllerVelocity")
findProject(":LaneController:LaneControllerVelocity")?.name = "LaneControllerVelocity"
