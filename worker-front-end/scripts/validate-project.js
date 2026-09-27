const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');
const { compileScript, compileStyle, compileTemplate, parse } = require('@vue/compiler-sfc');
const JSON5 = require('json5');

const root = path.resolve(__dirname, '..');
const ignoredDirectories = new Set(['node_modules', 'uni_modules', 'unpackage']);

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
  const source = fs.readFileSync(file, 'utf8');
  const result = spawnSync(process.execPath, ['--input-type=module', '--check'], {
    input: source,
    encoding: 'utf8'
  });
  if (result.status !== 0) errors.push(`${relative(file)}: ${result.stderr.trim()}`);
}

const jsonFiles = walk(root, '.json');
for (const file of jsonFiles) {
  try {
    JSON5.parse(fs.readFileSync(file, 'utf8'));
  } catch (error) {
    errors.push(`${relative(file)}: ${error.message}`);
  }
}

const vueFiles = walk(root, '.vue');
for (const file of vueFiles) {
  const source = fs.readFileSync(file, 'utf8');
  const id = relative(file);
  const parsed = parse(source, { filename: file });
  for (const error of parsed.errors) errors.push(`${id}: ${String(error)}`);
  if (parsed.errors.length > 0) continue;

  const descriptor = parsed.descriptor;
  try {
    if (descriptor.script || descriptor.scriptSetup) compileScript(descriptor, { id });
  } catch (error) {
    errors.push(`${id}: ${error.message}`);
  }
  if (descriptor.template) {
    const result = compileTemplate({ source: descriptor.template.content, filename: file, id });
    for (const error of result.errors) errors.push(`${id}: ${String(error)}`);
  }
  for (const style of descriptor.styles) {
    const result = compileStyle({
      source: style.content,
      filename: file,
      id,
      scoped: style.scoped,
      preprocessLang: style.lang
    });
    for (const error of result.errors) errors.push(`${id}: ${String(error)}`);
  }
}

const pagesConfig = JSON5.parse(fs.readFileSync(path.join(root, 'pages.json'), 'utf8'));
for (const page of pagesConfig.pages || []) {
  const file = path.join(root, `${page.path}.vue`);
  if (!fs.existsSync(file)) errors.push(`${page.path}.vue: pages.json 引用的页面文件不存在`);
}

if (errors.length > 0) {
  console.error(errors.join('\n'));
  process.exit(1);
}

console.log(
  `Validated ${jsFiles.length} JS files, ${jsonFiles.length} JSON files and ${vueFiles.length} Vue files.`
);
