const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const root = path.resolve(__dirname, '..');
const ignoredDirectories = new Set(['node_modules', 'miniprogram_npm']);

function walk(directory, extension, files = []) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (entry.isDirectory() && ignoredDirectories.has(entry.name)) continue;
    const absolutePath = path.join(directory, entry.name);
    if (entry.isDirectory()) walk(absolutePath, extension, files);
    else if (entry.name.endsWith(extension)) files.push(absolutePath);
  }
  return files;
}

function relative(file) {
  return path.relative(root, file).replaceAll('\\', '/');
}

const errors = [];
const jsFiles = walk(root, '.js');
for (const file of jsFiles) {
  const result = spawnSync(process.execPath, ['--check', file], { encoding: 'utf8' });
  if (result.status !== 0) errors.push(`${relative(file)}: ${result.stderr.trim()}`);
}

const jsonFiles = walk(root, '.json');
for (const file of jsonFiles) {
  try {
    JSON.parse(fs.readFileSync(file, 'utf8'));
  } catch (error) {
    errors.push(`${relative(file)}: ${error.message}`);
  }
}

const appConfig = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'));
for (const page of appConfig.pages || []) {
  for (const extension of ['.js', '.json', '.wxml']) {
    const file = path.join(root, `${page}${extension}`);
    if (!fs.existsSync(file)) errors.push(`${page}${extension}: app.json 引用的页面文件不存在`);
  }
}

if (errors.length > 0) {
  console.error(errors.join('\n'));
  process.exit(1);
}

console.log(
  `Validated ${jsFiles.length} JS files, ${jsonFiles.length} JSON files and ${appConfig.pages.length} pages.`
);
