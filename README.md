TrackTwist App Project Outline

I. Project Description
TrackTwist is a mobile music recommendation application developed for Android devices. The app provides users with randomized music sample recommendations based on their selected genre. It demonstrates core Android development skills, user experience design, and potential for future feature expansion. The minimum viable product uses locally stored audio samples and metadata with a focus on fast, intuitive interaction.

II. Problem Addressing
Modern streaming services offer vast music libraries but often overwhelm users with options. Many platforms require paid subscriptions or time-consuming searches to discover new music. TrackTwist addresses this challenge by delivering simple, randomized genre-based samples without unnecessary complexity. The app allows users to explore new music quickly and without account creation or payment, meeting the need for a lightweight discovery tool.

III. Platform
•	Operating System: Android
•	Development Environment: Android Studio running in a Linux-based virtual machine
•	Language: Java (with potential for Kotlin in future updates)
•	Build System: Gradle
•	Device Support: Tested on emulator and physical Android devices
•	Compatibility Targets: Compile SDK 36 and minimum SDK 24 for broad device coverage

IV. Front-End and Back-End Support
•	Front-End (Client):
o	XML layouts with responsive design
o	Core screens include:
	Home screen with genre spinner and spin button
	Player/Detail screen with album art, playback controls, and like/dislike buttons
	Favorites screen for saved tracks
	About and Help screens with app and developer information
o	Color scheme and UI designed for accessibility and consistency
•	Back-End (Logic and Data):
o	Local storage of music sample metadata using JSON or SQLite
o	Playback handled with Android’s MediaPlayer API
o	No external server dependency in the MVP
o	Future enhancement path for cloud storage or API integration (e.g., Deezer 30-second previews)

V. Functionality
•	Genre selection using a spinner control
•	Randomized sample playback within the selected genre
•	Display of track information including artist and title
•	Playback controls: play, pause, next, and previous
•	Like/Dislike buttons for saving or skipping recommendations
•	Simple sharing option for track recommendations
•	Planned post-MVP features: Deezer API integration, expanded track library, cloud sync for favorites

VI. Design (Wireframes)
•	Home Screen: Genre spinner at the top, a central spin button, and a display area for the recommended track
•	Player/Detail Screen: Album art placeholder, playback controls, thumbs up/down buttons, and track metadata
•	Favorites Screen: List view of saved tracks with options for playback or removal
•	About and Help Screens: Description of the app and usage instructions
•	Design Approach: Minimalist layout with a navy-to-blue gradient background, consistent color palette for buttons and icons, and a clean layout that avoids clutter. Wireframes and screenshots will be added as they are finalized.

VII. GitHub Integration
•	Repository: tracktwist
o	Contains complete source code, documentation, and changelog
•	README.md:
o	Includes project overview, installation/setup instructions, usage, and planned enhancements
•	Wiki:
o	Publishes this formal project outline
•	Submission Requirements:
o	Downloadable copy of README for submission
o	Link to GitHub README and Wiki provided with the outline report
________________________________________

