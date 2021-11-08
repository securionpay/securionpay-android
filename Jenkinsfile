pipeline {
    agent {
        label "xcloud"
    }
    environment {
        LC_CTYPE = 'en_US.UTF-8'
        PATH = '/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin'
    }
    options {
        ansiColor('xterm')
        disableConcurrentBuilds()
    }

    stages {
        stage("Prepare environment") {
            steps {
                checkout scm

                script {
                    sh '''#!/bin/bash -l

                    '''
                }
            }
        }
        stage("Build") {
            steps {
                checkout scm

                script {
                    sh '''#!/bin/bash -l
                        echo "sdk.dir=/Users/jenkins/Library/Android/sdk" > local.properties
                        export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_291.jdk/Contents/Home
                        export ANDROID_SDK=/Users/jenkins/Library/Android/sdk/

                        echo "System configuration:"
                        echo "OSX version:"
                        defaults read loginwindow SystemVersionStampAsString
                        echo "Available emulators:"
                        $ANDROID_SDK/emulator/emulator -list-avds

                        ./gradlew clean test --info

                        rm -rf unit_test_results
                        mkdir unit_test_results
                        mv securionpay/build/test-results/testDebugUnitTest unit_test_results
                        mv securionpay/build/test-results/testReleaseUnitTest unit_test_results

                        ./run_ui_tests.sh Pixel_3a_API_30_x86
                    '''
                }
            }
        }
    }

    post {
        always {
            junit 'unit_test_results/testDebugUnitTest/TEST**.xml'
            junit 'unit_test_results/testReleaseUnitTest/TEST**.xml'
            junit 'example/build/outputs/androidTest-results/connected/TEST**.xml'
        }
    }
}
