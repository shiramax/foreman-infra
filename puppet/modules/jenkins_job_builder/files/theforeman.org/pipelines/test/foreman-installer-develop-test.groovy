def commit_hash = ''

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 2, unit: 'HOURS')
        ansiColor('xterm')
    }

    stages {
        stage("Collect Git Hash") {
            steps {
                git(url: 'https://github.com/theforeman/foreman-installer', branch: 'develop')
                script {
                    commit_hash = archive_git_hash()
                }
            }
        }
        stage("test ruby 2.0 & puppet 5") {
            steps {
                run_test(ruby: '2.0.0', puppet: '5.5')
            }
        }
        stage("test ruby 2.4 & puppet 5") {
            steps {
                run_test(ruby: '2.4', puppet: '5.5')
            }
        }
        stage("test ruby 2.5 & puppet 6") {
            steps {
                run_test(ruby: '2.5', puppet: '6.3')
            }
        }
        stage("Release Nightly Package") {
            steps {
                build(
                    job: 'foreman-installer-develop-release',
                    propagate: false,
                    wait: false,
                    parameters: [
                        string(name: 'git_ref', value: commit_hash)
                    ]
                )
            }
        }
    }
    post {
        always {
            deleteDir()
        }
    }
}

def run_test(args) {
    def ruby = args.ruby
    def puppet = args.puppet
    def gemset = "ruby-${ruby}-puppet-${puppet}"

    try {
        configureRVM(ruby, gemset)
        withRVM(["PUPPET_VERSION='${puppet}' bundle install --without=development --jobs=5 --retry=5"], ruby, gemset)
        withRVM(["PUPPET_VERSION='${puppet}' bundle exec rake spec"], ruby, gemset)
    } finally {
        cleanupRVM(ruby, gemset)
    }
}
