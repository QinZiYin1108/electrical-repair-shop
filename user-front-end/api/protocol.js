const request = require('./request');

function fetchProtocol(type) {
  return request({
    url: `/pass/protocols/${type}`,
    method: 'GET'
  });
}

module.exports = {
  fetchProtocol
};
