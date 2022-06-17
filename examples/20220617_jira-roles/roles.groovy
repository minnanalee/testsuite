//隐藏move菜单功能
//jira-administrators,和项目administrators是白名单

import com.atlassian.jira.ComponentManager
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.security.roles.ProjectRoleActors
import com.atlassian.jira.security.roles.ProjectRoleManager


if (ComponentAccessor.getGroupManager().getGroupsForUser(currentUser)?.find { it.name == "jira-administrators" }) {
    return false
}

ComponentManager componentManager = ComponentManager.getInstance()
ProjectManager projectManager = componentManager.getProjectManager()
ProjectRoleManager projectRoleManager =  ComponentAccessor.getComponent(ProjectRoleManager)

// name of role here
ProjectRole adminRole = projectRoleManager.getProjectRole("Project Administrators")
def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

if (projectRoleManager.isUserInProjectRole(currentUser, adminRole, underlyingIssue.getProjectObject())) {
    return false
}