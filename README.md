# dollar-recognizer-wear

Android studio project for implementation(partial) of $1 recognizer http://depts.washington.edu/aimgroup/proj/dollar/index.html for wearable devices. The wearable uses a modified version of https://github.com/saxenasaurabh/drawpath for drawing gestures. These gestures are sent to the handheld device which uses http://depts.washington.edu/aimgroup/proj/dollar/dollar.js to recognize the gesture. The recognized result is sent back to the wearable and is displayed in a blue textView below the drawing canvas. The mobile app shows the path that was used for generating the result.

To exit the wearable app:
The wearable app disables the default swipe-from-left exit action. To exit, long press the blue textView on the bottom. This should show the dismiss button.

What's not here?
- Training new gestures

Also this has not been tested for round watch faces.

Feedback is welcome.
