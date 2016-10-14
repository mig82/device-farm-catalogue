#!/usr/bin/groovy

/*
 * Copyright (c) 2016, Andrey Makeev <amaksoft@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice unmodified, this list of conditions, and the following
 *    disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and|or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Method for pushing build results to git repo via SSH. (SSH Agent Plugin required)
 * To keep things going while we wait for official Git Publish support for pipelines (https://issues.jenkins-ci.org/browse/JENKINS-28335)
 *
 * Example call (Inline values):
 * pushSSH(branch: "master", commitMsg: "Jenkins build #${env.BUILD_NUMBER}", tagName: "build-${env.BUILD_NUMBER}", files: ".", config: true, username: "Jenkins CI", email: "jenkins-ci@example.com");
 *
 * Example call (Environment variables):
 * env.BRANCH_NAME = "mycoolbranch"// BRANCH_NAME is predefined in multibranch pipeline job
 * env.J_GIT_CONFIG = "true"
 * env.J_USERNAME = "Jenkins CI"
 * env.J_EMAIL = "jenkins-ci@example.com"
 * env.J_CREDS_IDS = '02aa92ec-593e-4a90-ac85-3f43a06cfae3' // Use credentials id from Jenkins (Does anyone know a way to reference them by name rather than by id?)
 * ...
 * pushSSH(commitMsg: "Jenkins build #${env.BUILD_NUMBER}", tagName: "build-${env.BUILD_NUMBER}", files: ".");
 *
 * @param args Map with followinf parameters:
 *   commitMsg : (String) commit message
 *   files : (String) list of files to push (space serparated) (Won't push files if not specified)
 *   tagName : (String) tag name (won't push tag if not specified)
 *   branch : (String) git branch (Will use env variable BRANCH_NAME if not specified)
 *   creds_ids : (List<String>) credentials ids (Will use env variable J_CREDS_IDS if not specified) (haven't figured out yet how to resolve credentials name)
 *   configure : (boolean) configure git publisher (username, email). (If not specified will check out env variable J_GIT_CONFIG)
 *   username : (String) committer name (If not specified will check out env variable J_USERNAME)
 *   email : (String) committer email (If not specified will check out env variable J_EMAIL)
 */
 
def pushSSH(Map args) {

    String tagName = args.tagName
    String commitMsg = args.commitMsg
    String files = args.files
    String branch = args.branch != null ? args.branch : env.BRANCH_NAME;
    List<String> creds_ids = args.creds != null ? args.creds : env.J_CREDS_IDS.tokenize(" ");
    boolean config; // Boolean.parseBoolean() is forbidden in this DSL
    if(args.config != null)
        config = args.config
    else if (env.J_GIT_CONFIG.toLowerCase() == "true") {
        config = true
    }else {
        echo "git config = ${config}, J_GIT_CONFIG = ${env.J_GIT_CONFIG}, assuming false"
        config = false;
    }
    String username = args.username != null ? args.username : env.J_USERNAME;
    String email = args.email != null ? args.email : env.J_EMAIL;

    if (tagName == null && files == null) {
        echo "Neither tag nor files to push specified. Ignoring.";
        return;
    }

    if (branch == null)
        error "Error. Invalid value: git branch = ${branch}";

    if(config) {
        if (username == null || email == null || creds_ids == null)
            error "Error. Invalid value set: { username = ${username}, email = ${email}, credentials = ${creds_ids} }";
        sh """ git config push.default simple
               git config user.name \"${username}\"
               git config user.email \"${email}\"
           """
    }

    sshagent(creds_ids) {
        if (files != null) {
            sh """ git add . && git commit -m \"${commitMsg}\" || true
                   git push origin HEAD:refs/heads/${branch} || true
               """
        }
        if (tagName != null) {
            sh """ git tag -fa \"${tagName}\" -m \"${commitMsg}\"
                   git push -f origin refs/tags/${tagName}:refs/tags/${tagName}
               """
        }
    }
}

return this;