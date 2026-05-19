/**
 * IVIDS Music - Multi-Platform Release Packager
 * 
 * Description: Automates release version configurations, compiles Mobile, TV, and PC binaries,
 *              and uses a Git orphan branch sequence to create a release tag containing ONLY
 *              the compiled binaries at its root.
 * 
 * Usage:
 *   node .agents/scripts/build-release.js <versionName> [versionCode]
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Target paths relative to project root
const ROOT_DIR = path.join(__dirname, '../..');
const GRADLE_PATH = path.join(ROOT_DIR, 'app/build.gradle.kts');
const UPDATER_PATH = path.join(ROOT_DIR, 'app/src/main/assets/logic/updater.js');
const SETTINGS_PATH = path.join(ROOT_DIR, 'app/src/main/assets/gui/pages/settings.html');
const PACKAGE_PATH = path.join(ROOT_DIR, 'package.json');

// Helper to run shell commands synchronously and print outputs
function runCommand(command, errorMessage) {
    console.log(`Running: ${command}`);
    try {
        execSync(command, { cwd: ROOT_DIR, stdio: 'inherit' });
        return true;
    } catch (error) {
        console.error(`\x1b[31m[Error] ${errorMessage}\x1b[0m`);
        return false;
    }
}

function main() {
    const args = process.argv.slice(2);
    if (args.length === 0 || args[0] === '--help' || args[0] === '-h') {
        console.log('\n\x1b[35m=== IVIDS Music Release Packager ===\x1b[0m');
        console.log('Usage:');
        console.log('  node .agents/scripts/build-release.js <versionName> [versionCode]');
        console.log('\nExamples:');
        console.log('  node .agents/scripts/build-release.js 1.2.0      (Auto-increments versionCode)');
        console.log('  node .agents/scripts/build-release.js 1.2.0 8    (Sets versionCode to 8)\n');
        process.exit(0);
    }

    const targetVersionName = args[0].replace(/^v/, ''); // Clean leading 'v'
    let targetVersionCode = args[1] ? parseInt(args[1], 10) : null;

    console.log(`\n\x1b[36m[Release] Starting release process for version v${targetVersionName}...\x1b[0m`);

    // ==========================================
    // 1. Sync Version Names Across Source Code
    // ==========================================

    // A. Update build.gradle.kts
    if (fs.existsSync(GRADLE_PATH)) {
        let gradleContent = fs.readFileSync(GRADLE_PATH, 'utf8');
        const codeRegex = /versionCode\s*=\s*(\d+)/;
        const nameRegex = /versionName\s*=\s*"([^"]+)"/;
        const codeMatch = gradleContent.match(codeRegex);

        if (codeMatch) {
            const currentVersionCode = parseInt(codeMatch[1], 10);
            if (!targetVersionCode) {
                targetVersionCode = currentVersionCode + 1;
            }
            gradleContent = gradleContent
                .replace(codeRegex, `versionCode = ${targetVersionCode}`)
                .replace(nameRegex, `versionName = "${targetVersionName}"`);
            fs.writeFileSync(GRADLE_PATH, gradleContent, 'utf8');
            console.log(`\x1b[32m✔ Updated build.gradle.kts (versionCode = ${targetVersionCode}, versionName = "${targetVersionName}").\x1b[0m`);
        }
    }

    // B. Update package.json
    if (fs.existsSync(PACKAGE_PATH)) {
        let packageJson = JSON.parse(fs.readFileSync(PACKAGE_PATH, 'utf8'));
        packageJson.version = targetVersionName;
        fs.writeFileSync(PACKAGE_PATH, JSON.stringify(packageJson, null, 2), 'utf8');
        console.log(`\x1b[32m✔ Updated package.json (version = "${targetVersionName}").\x1b[0m`);
    }

    // C. Update updater.js
    if (fs.existsSync(UPDATER_PATH)) {
        let updaterContent = fs.readFileSync(UPDATER_PATH, 'utf8');
        const updaterRegex = /const CURRENT_VERSION\s*=\s*'([^']+)'/;
        if (updaterContent.match(updaterRegex)) {
            updaterContent = updaterContent.replace(updaterRegex, `const CURRENT_VERSION = '${targetVersionName}'`);
            fs.writeFileSync(UPDATER_PATH, updaterContent, 'utf8');
            console.log(`\x1b[32m✔ Updated updater.js (CURRENT_VERSION = '${targetVersionName}').\x1b[0m`);
        }
    }

    // D. Update settings.html
    if (fs.existsSync(SETTINGS_PATH)) {
        let settingsContent = fs.readFileSync(SETTINGS_PATH, 'utf8');
        const settingsRegex = /<div class="settings-desc">[^<]+<\/div>/;
        if (settingsContent.match(settingsRegex)) {
            settingsContent = settingsContent.replace(settingsRegex, `<div class="settings-desc">${targetVersionName} (Beta)</div>`);
            fs.writeFileSync(SETTINGS_PATH, settingsContent, 'utf8');
            console.log(`\x1b[32m✔ Updated settings.html (Version label set to '${targetVersionName} (Beta)').\x1b[0m`);
        }
    }

    // Commit changes on main
    console.log('\nCommitting version synchronization updates on main branch...');
    runCommand('git add .', 'Failed to stage version configuration updates');
    runCommand(`git commit -m "chore: bump version configurations to v${targetVersionName}"`, 'Failed to commit version updates');

    // ==========================================
    // 2. Compile Release Build Variants
    // ==========================================

    console.log('\nCompiling Mobile Release APK...');
    if (!runCommand('cmd /c gradlew.bat assembleMobileRelease', 'Failed to compile Mobile APK')) {
        process.exit(1);
    }

    console.log('\nCompiling TV Release APK...');
    if (!runCommand('cmd /c gradlew.bat assembleTvRelease', 'Failed to compile TV APK')) {
        process.exit(1);
    }

    console.log('\nCompiling PC Desktop Portable EXE...');
    if (!runCommand('npm run dist', 'Failed to package PC Desktop executable')) {
        process.exit(1);
    }

    // ==========================================
    // 3. Relocate Compiled Binaries to Temp Storage
    // ==========================================

    const tempOutputDir = path.join(ROOT_DIR, '../ividsmusic_release_temp');
    if (!fs.existsSync(tempOutputDir)) {
        fs.mkdirSync(tempOutputDir, { recursive: true });
    }

    // Define source paths
    const mobileSource = path.join(ROOT_DIR, 'app/build/outputs/apk/mobile/release/app-mobile-release-unsigned.apk');
    const tvSource = path.join(ROOT_DIR, 'app/build/outputs/apk/tv/release/app-tv-release-unsigned.apk');
    
    // Find PC build output (e.g. dist/ividsmusic 0.1.6 Portable.exe)
    const distFiles = fs.readdirSync(path.join(ROOT_DIR, 'dist'));
    const pcSourceFile = distFiles.find(file => file.endsWith('.exe'));
    const pcSource = pcSourceFile ? path.join(ROOT_DIR, 'dist', pcSourceFile) : null;

    // Verify presence of all three built files
    if (!fs.existsSync(mobileSource) || !fs.existsSync(tvSource) || !pcSource || !fs.existsSync(pcSource)) {
        console.error('\x1b[31m[Error] One or more compiled release files were missing. Release aborted.\x1b[0m');
        process.exit(1);
    }

    // Copy to temporary output path
    const tempMobile = path.join(tempOutputDir, 'IVIDSMusic_Mobile.apk');
    const tempTv = path.join(tempOutputDir, 'IVIDSMusic_TV.apk');
    const tempPc = path.join(tempOutputDir, 'IVIDSMusic_PC.exe');

    fs.copyFileSync(mobileSource, tempMobile);
    fs.copyFileSync(tvSource, tempTv);
    fs.copyFileSync(pcSource, tempPc);

    console.log('\n\x1b[32m✔ Compiled binaries successfully stored in temporary directory.\x1b[0m');

    // ==========================================
    // 4. Git Orphan Isolation Flow
    // ==========================================

    console.log('\nInitiating Git orphan isolation flow...');
    
    // Switch to orphan branch
    if (!runCommand('git checkout --orphan temp-release-run', 'Failed to checkout orphan branch')) {
        process.exit(1);
    }

    // Clean all directories in orphan branch
    if (!runCommand('git rm -rf .', 'Failed to clean orphan branch directory')) {
        // Safe recovery fallback: switch back to main
        runCommand('git checkout main', 'Failed to return to main branch');
        process.exit(1);
    }

    // Copy binaries from temp to root
    fs.copyFileSync(tempMobile, path.join(ROOT_DIR, 'IVIDSMusic_Mobile.apk'));
    fs.copyFileSync(tempTv, path.join(ROOT_DIR, 'IVIDSMusic_TV.apk'));
    fs.copyFileSync(tempPc, path.join(ROOT_DIR, 'IVIDSMusic_PC.exe'));

    // Commit changes to the orphan branch
    if (!runCommand('git add IVIDSMusic_Mobile.apk IVIDSMusic_TV.apk IVIDSMusic_PC.exe', 'Failed to stage binaries in orphan branch')) {
        runCommand('git checkout main', 'Failed to return to main branch');
        process.exit(1);
    }

    if (!runCommand(`git commit -m "Release v${targetVersionName}"`, 'Failed to commit binaries')) {
        runCommand('git checkout main', 'Failed to return to main branch');
        process.exit(1);
    }

    // Create the tag
    const tagName = `v${targetVersionName}`;
    // Delete tag locally if it already exists to avoid conflict
    try {
        execSync(`git tag -d ${tagName}`, { stdio: 'ignore' });
    } catch (e) {}

    if (!runCommand(`git tag -a ${tagName} -m "Release ${tagName}"`, 'Failed to tag release commit')) {
        runCommand('git checkout main', 'Failed to return to main branch');
        process.exit(1);
    }

    // Switch back to main
    console.log('\nRestoring main branch codebase...');
    runCommand('git checkout main', 'Failed to restore main branch');
    runCommand('git branch -D temp-release-run', 'Failed to delete temporary packaging branch');

    // Clean temporary storage directory
    try {
        fs.rmSync(tempOutputDir, { recursive: true, force: true });
    } catch (e) {}

    console.log(`\n\x1b[35m=== RELEASE PREPARATION COMPLETE ===\x1b[0m`);
    console.log(`Local tag \x1b[32m${tagName}\x1b[0m created successfully.`);
    console.log('The tag points to a commit containing ONLY the 3 distribution binaries in its root.');
    console.log('The main branch retains the complete source code.');
    console.log(`\nTo push and publish this release, execute:`);
    console.log(`  \x1b[36mgit push origin main\x1b[0m`);
    console.log(`  \x1b[36mgit push origin ${tagName}\x1b[0m\n`);
}

main();
