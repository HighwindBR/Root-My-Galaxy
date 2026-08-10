JAVA_HOME ?= /Applications/Android Studio.app/Contents/jbr/Contents/Home
GRADLEW := ./gradlew
APK := app/build/outputs/apk/debug/app-debug.apk
PACKAGE := dev.busung.s25uroot
ACTIVITY := .MainActivity
SOURCES := $(shell find app/src -type f) build.gradle.kts settings.gradle.kts gradle.properties

.PHONY: all install run clean reverse

all: $(APK)

$(APK): $(SOURCES)
	JAVA_HOME="$(JAVA_HOME)" $(GRADLEW) :app:assembleDebug

install: $(APK)
	adb install -r $(APK)
	adb shell am start -n $(PACKAGE)/$(ACTIVITY)

run: install

reverse:
	adb reverse tcp:8080 tcp:8080

clean:
	JAVA_HOME="$(JAVA_HOME)" $(GRADLEW) clean
