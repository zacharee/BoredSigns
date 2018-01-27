# BoredSigns
BoredSigns is an app for the LG V20 and V10, which aims to add further functionality to the second screen, in the form of extra widgets.
It won't and can't work on any other devices.

# Privacy
BoredSigns requires the use of an Accessibility service to allow the Navigation Bar widget to function. The service is only used for the device control actions available in the previously mentioned widget, and does not collect any information of any kind.

BoredSigns requires notification access to allow the Information widget's notification feature to function. BoredSigns does not read any information from these notifications, beyond the package names and icons of the notifying apps.

BoredSigns needs the READ_PHONE_STATE permission. This is for the Information widget's mobile signal feature. It is not used to collect any information beyond the current signal level.

# Building Yourself
The latest Android Studio Canary is required to build this.

There is also a class that is not uploaded to GitHub. This class is merely used to verify the license of users who buy this app from the Play Store. Before building, modify AndroidManifest.xml to remove the `android:name` field in the `<application>` tag. Neglecting to do so will result in a crash on launch.

# License

    Copyright (C) 2018  Zachary Wander

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
