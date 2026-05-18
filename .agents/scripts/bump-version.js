/**
 * IVIDS Music - Release Version Auto-Bumper Script
 * 
 * Description: Automates release version configurations across the Android Gradle configuration,
 *              the JavaScript updater module, and the settings dashboard UI, and appends the 
 *              updates to CHANGELOG.md.
 * 
 * Usage:
 *   node .agents/scripts/bump-version.js <versionName> [versionCode]
 * 
 * Examples:
 *   node .agents/scripts/bump-version.js 0.1.5      (Auto-increments versionCode by 1)
 *   node .agents/scripts/bump-version.js 0.1.5 7    (Sets versionCode explicitly to 7)
 */

const fs = require('fs');
const path = require('path');

// Target paths relative to project root
const GRADLE_PATH = path.join(__dirname, '../../app/build.gradle.kts');
const UPDATER_PATH = path.join(__dirname, '../../app/src/main/assets/logic/updater.js');
const SETTINGS_PATH = path.join(__dirname, '../../app/src/main/assets/gui/pages/settings.html');
const CHANGELOG_PATH = path.join(__dirname, '../../documentation/CHANGELOG.md');

// Validate command line arguments
const args = process.argv.slice(2);
if (args.length === 0 || args[0] === '--help' || args[0] === '-h') {
    console.log('\n\x1b[35m=== IVIDS Music Version Auto-Bumper ===\x1b[0m');
    console.log('Usage:');
    console.log('  node .agents/scripts/bump-version.js <versionName> [versionCode]');
    console.log('\nExamples:');
    console.log('  node .agents/scripts/bump-version.js 0.1.5      (Bumps version to 0.1.5 and increments code by 1)');
    console.log('  node .agents/scripts/bump-version.js 0.1.5 7    (Bumps version to 0.1.5 and sets code to 7)\n');
    process.exit(0);
}

const targetVersionName = args[0].replace(/^v/, ''); // Clean leading 'v' if entered
let targetVersionCode = args[1] ? parseInt(args[1], 10) : null;

if (args[1] && isNaN(targetVersionCode)) {
    console.error('\x1b[31mError: Version code must be a valid integer.\x1b[0m');
    process.exit(1);
}

console.log(`\n\x1b[36m[Bumper] Initiating version bump to: v${targetVersionName}...\x1b[0m`);

// === 1. Parse and Update build.gradle.kts ===
if (!fs.existsSync(GRADLE_PATH)) {
    console.error(`\x1b[31mError: Gradle file not found at ${GRADLE_PATH}\x1b[0m`);
    process.exit(1);
}

let gradleContent = fs.readFileSync(GRADLE_PATH, 'utf8');

// Match version elements
const codeRegex = /versionCode\s*=\s*(\d+)/;
const nameRegex = /versionName\s*=\s*"([^"]+)"/;

const codeMatch = gradleContent.match(codeRegex);
const nameMatch = gradleContent.match(nameRegex);

if (!codeMatch || !nameMatch) {
    console.error('\x1b[31mError: Could not parse versionCode or versionName from build.gradle.kts\x1b[0m');
    process.exit(1);
}

const currentVersionCode = parseInt(codeMatch[1], 10);
const currentVersionName = nameMatch[1];

if (!targetVersionCode) {
    targetVersionCode = currentVersionCode + 1;
}

console.log(`[Gradle] Bumping versionCode: ${currentVersionCode} -> ${targetVersionCode}`);
console.log(`[Gradle] Bumping versionName: "${currentVersionName}" -> "${targetVersionName}"`);

gradleContent = gradleContent
    .replace(codeRegex, `versionCode = ${targetVersionCode}`)
    .replace(nameRegex, `versionName = "${targetVersionName}"`);

fs.writeFileSync(GRADLE_PATH, gradleContent, 'utf8');
console.log('\x1b[32m✔ Updated build.gradle.kts successfully.\x1b[0m');


// === 2. Update updater.js ===
if (!fs.existsSync(UPDATER_PATH)) {
    console.error(`\x1b[31mError: Updater script not found at ${UPDATER_PATH}\x1b[0m`);
    process.exit(1);
}

let updaterContent = fs.readFileSync(UPDATER_PATH, 'utf8');
const updaterRegex = /const CURRENT_VERSION\s*=\s*'([^']+)'/;

if (!updaterContent.match(updaterRegex)) {
    console.error('\x1b[31mError: Could not parse CURRENT_VERSION from updater.js\x1b[0m');
    process.exit(1);
}

updaterContent = updaterContent.replace(updaterRegex, `const CURRENT_VERSION = '${targetVersionName}'`);
fs.writeFileSync(UPDATER_PATH, updaterContent, 'utf8');
console.log('\x1b[32m✔ Updated updater.js successfully.\x1b[0m');


// === 3. Update settings.html ===
if (!fs.existsSync(SETTINGS_PATH)) {
    console.error(`\x1b[31mError: settings.html not found at ${SETTINGS_PATH}\x1b[0m`);
    process.exit(1);
}

let settingsContent = fs.readFileSync(SETTINGS_PATH, 'utf8');
const settingsRegex = /<div class="settings-desc">[^<]+<\/div>/;

if (!settingsContent.match(settingsRegex)) {
    console.error('\x1b[31mError: Could not locate version display tag in settings.html\x1b[0m');
    process.exit(1);
}

settingsContent = settingsContent.replace(settingsRegex, `<div class="settings-desc">${targetVersionName} (Beta)</div>`);
fs.writeFileSync(SETTINGS_PATH, settingsContent, 'utf8');
console.log('\x1b[32m✔ Updated settings.html successfully.\x1b[0m');


// === 4. Auto-log to CHANGELOG.md ===
if (fs.existsSync(CHANGELOG_PATH)) {
    const today = new Date().toISOString().split('T')[0];
    let changelogContent = fs.readFileSync(CHANGELOG_PATH, 'utf8');

    const logEntries = [
        `[${today}] EDITED app/src/main/assets/gui/pages/settings.html: Bumped visual version label to ${targetVersionName} (Beta) inside settings dashboard for UI consistency`,
        `[${today}] EDITED app/src/main/assets/logic/updater.js: Bumped CURRENT_VERSION constant to ${targetVersionName} to coordinate with v${targetVersionName} release`,
        `[${today}] EDITED app/build.gradle.kts: Bumped versionName to v${targetVersionName} and incremented versionCode to ${targetVersionCode}.`
    ];

    const appendedText = '\n' + logEntries.join('\n') + '\n';
    changelogContent = changelogContent.trim() + appendedText;
    
    fs.writeFileSync(CHANGELOG_PATH, changelogContent, 'utf8');
    console.log('\x1b[32m✔ Appended granular version log entries to CHANGELOG.md successfully.\x1b[0m');
} else {
    console.log('\x1b[33m[Warning] CHANGELOG.md not found, skipping logs.\x1b[0m');
}

console.log('\n\x1b[35m[Success] Version configuration fully synchronized to v' + targetVersionName + '!\x1b[0m\n');
