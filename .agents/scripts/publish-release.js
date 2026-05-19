/**
 * IVIDS Music - Scoped GitHub Release Publisher
 *
 * Publishes an already-created orphan release tag without guessing whether
 * main should also be pushed.
 *
 * Usage:
 *   node .agents/scripts/publish-release.js v0.2.1
 *   node .agents/scripts/publish-release.js v0.2.1 --push-main
 */

const fs = require('fs');
const os = require('os');
const path = require('path');
const { execFileSync, spawnSync } = require('child_process');

const ROOT_DIR = path.join(__dirname, '../..');
const ASSET_NAMES = [
    'IVIDSMusic_Mobile.apk',
    'IVIDSMusic_TV.apk',
    'IVIDSMusic_PC.exe',
];

function run(command, args, options = {}) {
    const printable = [command, ...args].join(' ');
    console.log(`Running: ${printable}`);
    const result = spawnSync(command, args, {
        cwd: ROOT_DIR,
        stdio: 'inherit',
        shell: false,
        ...options,
    });

    if (result.status !== 0) {
        process.exit(result.status || 1);
    }
}

function capture(command, args) {
    return execFileSync(command, args, {
        cwd: ROOT_DIR,
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe'],
    }).trim();
}

function ensureCommand(command, args) {
    try {
        execFileSync(command, args, { cwd: ROOT_DIR, stdio: 'ignore' });
    } catch (error) {
        console.error(`[Error] Required command failed: ${command} ${args.join(' ')}`);
        process.exit(1);
    }
}

function ensureTag(tagName) {
    try {
        capture('git', ['rev-parse', '--verify', `${tagName}^{tag}`]);
    } catch (error) {
        console.error(`[Error] Annotated release tag '${tagName}' does not exist locally.`);
        console.error('Run node .agents/scripts/build-release.js <versionName> first.');
        process.exit(1);
    }
}

function ensureOrphanAssetsOnly(tagName) {
    const files = capture('git', ['ls-tree', '--name-only', tagName])
        .split(/\r?\n/)
        .filter(Boolean)
        .sort();
    const expected = [...ASSET_NAMES].sort();

    if (JSON.stringify(files) !== JSON.stringify(expected)) {
        console.error(`[Error] Tag '${tagName}' does not contain exactly the expected release assets.`);
        console.error(`Expected: ${expected.join(', ')}`);
        console.error(`Actual: ${files.join(', ') || '(empty)'}`);
        process.exit(1);
    }
}

function extractAssets(tagName) {
    const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), `ividsmusic-${tagName}-`));

    for (const assetName of ASSET_NAMES) {
        const assetBuffer = execFileSync('git', ['show', `${tagName}:${assetName}`], {
            cwd: ROOT_DIR,
            encoding: 'buffer',
            stdio: ['ignore', 'pipe', 'pipe'],
        });
        fs.writeFileSync(path.join(tempDir, assetName), assetBuffer);
    }

    return tempDir;
}

function releaseNotes(versionName) {
    return [
        `## IVIDS Music ${versionName} Beta`,
        '',
        'This release strengthens the multi-platform IVIDS Music experience across Android Mobile, Android TV, and PC Desktop.',
        '',
        '### Highlights',
        '- Improved update handling with expanded native UpdateManager support.',
        '- Refined Android Mobile UI styling for cards, player controls, and search interactions.',
        '- Updated platform build configuration for the current multi-target release pipeline.',
        '- Continued PC playback reliability improvements through Electron/API integration fixes.',
        '- Preserved the isolated release artifact flow so distribution tags contain only compiled binaries.',
        '',
        '### Distribution Assets',
        '- IVIDSMusic_Mobile.apk',
        '- IVIDSMusic_TV.apk',
        '- IVIDSMusic_PC.exe',
    ].join('\n');
}

function main() {
    const args = process.argv.slice(2);
    const versionArg = args.find(arg => !arg.startsWith('--'));
    const pushMain = args.includes('--push-main');

    const wantsHelp = args.includes('--help') || args.includes('-h');
    if (!versionArg || wantsHelp) {
        console.log('Usage:');
        console.log('  node .agents/scripts/publish-release.js v0.2.1');
        console.log('  node .agents/scripts/publish-release.js v0.2.1 --push-main');
        process.exit(wantsHelp ? 0 : 1);
    }

    const tagName = versionArg.startsWith('v') ? versionArg : `v${versionArg}`;
    const versionName = tagName.replace(/^v/, 'v');
    const title = `Release ${tagName} (Beta)`;
    const notesPath = path.join(os.tmpdir(), `ividsmusic-${tagName}-release-notes.md`);

    ensureCommand('gh', ['--version']);
    ensureTag(tagName);
    ensureOrphanAssetsOnly(tagName);

    fs.writeFileSync(notesPath, releaseNotes(versionName), 'utf8');
    const tempAssetDir = extractAssets(tagName);
    const assetPaths = ASSET_NAMES.map(assetName => path.join(tempAssetDir, assetName));

    if (pushMain) {
        run('git', ['push', 'origin', 'main']);
    } else {
        console.log('Skipping main push because --push-main was not provided.');
    }

    run('git', ['push', 'origin', tagName]);
    run('gh', [
        'release',
        'create',
        tagName,
        ...assetPaths,
        '--title',
        title,
        '--notes-file',
        notesPath,
    ]);

    fs.rmSync(tempAssetDir, { recursive: true, force: true });
    fs.rmSync(notesPath, { force: true });

    console.log(`Published ${title}.`);
}

main();
