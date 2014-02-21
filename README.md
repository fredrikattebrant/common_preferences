Common Preferences - org.eclipse.common_prefs
See https://bugs.eclipse.org/bugs/show_bug.cgi?id=334016 for more details.
Fixed issue as described in comment#23 "part 2" - https://bugs.eclipse.org/bugs/show_bug.cgi?id=334016#c23

Blog entry: http://fredrikattebrant.blogspot.se/2011/03/controlling-eclipse-preferences.html

---

Install using update site: https://github.com/fredrikattebrant/common_preferences/raw/master/org.eclipse.common_prefs.updatesite

---
In the preferences folder, there are a couple of preference files that can be used with following steps (asuming common_preferences have been installed ;-) ):

Window > Preferences > General > Common Preferences
Add > https://raw2.github.com/fredrikattebrant/common_preferences/master/preferences/editor_settings.epf

Note - that to actually set these preferences for an existing workspace, you need to change the "Type" from "init" to "force" and perform a "Load". After this, you can revert the "Type" to "init" since leaving it as "force" will overwrite any changes you do to the preferences later on.

Other preferences:

https://raw2.github.com/fredrikattebrant/common_preferences/master/preferences/compare+problems+background.epf
https://raw2.github.com/fredrikattebrant/common_preferences/master/preferences/egit.staging.columns.epf


