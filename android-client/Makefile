alld: debug installd run log

allr: clean release installr run

clean:
	ant clean

debug:
	ant debug

release:
	ant release

installr:
	ant installr

installd:
	ant installd

run:
	adb shell am start -a android.intent.action.MAIN -n com.nononsenseapps.linksgcm/.MainActivity

log:
	adb logcat

