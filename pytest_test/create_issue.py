import sys
from jira import JIRA

def build_custom_issue(summary,assignee_name, description_path,
                proj_key="JAB",
                issue_type="Bug",
                reporter_name="a.ovchinnikova",
                priority="Medium",
                labels=[]
                ):
    issue_fields = {
        "project"     : {"key": proj_key},
        "issuetype"   : {"name": issue_type},   # Bug Task Story Epic
        "summary"     : "[REGRESS][{}] Тесты падают на регрессе".format(summary),
        "reporter"    : {"name":reporter_name},
        "description" : open(description_path, "r").read(),
        "priority"    : {"name": priority}, # Highest High Medium Low Lowest Blocker
        "labels"      : labels,
        "assignee"    : {"name": assignee_name}
    }
    return issue_fields

def get_jira():
    jira_options = {'server': 'http://localhost:7070'}
    jira = JIRA(options=jira_options, basic_auth=("a.ovchinnikova", "qwerty"))
    return jira

def check_for_issue(summary):
    jira = get_jira()
    issues = jira.search_issues("project = JenkinsAutoBug AND summary ~ \"{}\"".format(summary))
    if issues:
        return sorted(issues, key=lambda x: x.id, reverse=True)[0]
    else:
        return None

def create_jira_issue(issue_fields):
    jira = get_jira()
    jira.create_issue(issue_fields)

input_values = sys.argv[1:]
existing_issue = check_for_issue(input_values[0])
if existing_issue:
    get_jira().add_comment(existing_issue, open(input_values[2], "r").read())
else:
    create_jira_issue(build_custom_issue(input_values[0],input_values[1],input_values[2]))
