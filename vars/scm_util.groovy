// TODO: for some reason, this file is so ugly that my eyes wanna escape the eye sockets
// refactor plox
import com.oradian.pipedream.Checkout
import com.oradian.pipedream.caching.hash.GitHasher

def getBranchParameter() { return 'gitBranch' }

//Generated by snippet generator
private def config(
        remotes,
        branches) {
    return [
        changelog: false,
        poll: false,
        scm: [
            $class: 'GitSCM',
            branches: branches,
            doGenerateSubmoduleConfigurations: false,
            extensions: [
                [
                    $class: 'CloneOption',
                    honorRefspec: true,
                    noTags: true,
                    reference: '',
                    shallow: true,
                    depth: 1,
                    timeout: 10
                ]
            ],
            submoduleCfg: [],
            userRemoteConfigs: remotes,
        ]
    ]
}

def _checkout(def config) {
    (new Checkout(this)).checkout(config)
}

// Useful for builds from Pull Requests that have SCM global variable
// Take the remote and branch from PR, but use our config to clone
def clone() {
    def _config = null
    try {
        _config = config(scm.userRemoteConfigs, scm.branches)
    } catch (ex) {
        throw new Exception("Global 'scm' variable not set. This function is only intended to be used from PullRequest webhook. Please use clone(url) or clone(url, branch) otherwise.", ex)
    }

    _checkout(_config)
}

def clone(String url) {
    clone(url, getBranch())
}

def clone(String url, String branchOriginal) {
    def branch = branchOriginal.trim()
    def refspec = "+refs/heads/$branch:refs/remotes/origin/$branch"
    if ((branch =~ /v([0-9]+\.){1,}[0-9]/ && env.FORCE_BRANCH != "true") || env.FORCE_TAG == "true")
        refspec = "+refs/tags/$branch:refs/remotes/origin/$branch"

    _checkout(config([[url: url, refspec: refspec]], [[name: "*/$branch"]]))
}

def parameters(String defaultBranch = 'master') {
    return [string(name: branchParameter, defaultValue: defaultBranch)]
}

def getForwardParameters() {
    return [build_util.forwardParameter(branchParameter)]
}

def getBranch() {
    try {
        // Pullrequest - global scm object
        def branches = scm.branches;
        if (branches.size() == 1)
            return branches[0].name
    } catch (_) {
        // Pullrequest - environment from bitbucket plugin
        if (env.GIT_BRANCH)
            return env.GIT_BRANCH
    }

    // Parameter from manual runs
    if (env[branchParameter])
        return env[branchParameter]

    throw new Exception("Can't find git branch in scm global variable, GIT_BRANCH env nor ${branchParameter} env")
}

def getSha1(String path = null) {
    def hasher = new GitHasher(this)
    return hasher.getSha1(path)
}