# template-project-plugin

The aim of this plugin is to be able to use builders, publishers
and SCM settings from another project.

More documentation available on the Jenkins wiki:
https://wiki.jenkins-ci.org/display/JENKINS/Template+Project+Plugin
https://issues.jenkins-ci.org/browse/JENKINS/component/15623/

## Setup
* Set up a template project that has all the settings you want to share. E.g. you  could create one with no SCM filled in, but with all the builders and publishers you want for all your projects. Its best to mark this project as disabled, since you are not actually going to run it.
* Then set up a concrete project. Configure the SCM as you want. Then select 'use  all the publishers from this project' and pick the template project. Ditto for the builders.

## Limitations
* General:
  * It may be using some plugins in ways that were not intended. Compatibility with all plugins is not guaranteed.
* It does not support project actions. That means that links that should be on the project page (e.g. 'latest test results') will not be there.
* Publishers:
  * Post-build publishers need to be 'self-contained', meaning they may not work if a publisher relies on configs in the template project.
* SCM:
  * Only supports build variables (not environment variables) as gets into infinite loop using `getEnvironment()` since it loops back to `getScm().buildEnvVars()`.
* It has had virtually no testing.
