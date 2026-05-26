const request = require('./request');

const fetchHomePublicData = () => {
  return request({
    url: '/pass/home/public',
    method: 'GET'
  });
};

const fetchHomePrivateData = () => {
  return request({
    url: '/user/home/private',
    method: 'GET'
  });
};

module.exports = {
  fetchHomePublicData,
  fetchHomePrivateData
};

