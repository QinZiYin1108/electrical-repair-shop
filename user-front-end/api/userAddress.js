const request = require("./request");

const listUserAddresses = () => {
  return request({
    url: "/user/addresses/list",
    method: "GET"
  });
};

const getUserAddressDetail = (addressId) => {
  return request({
    url: "/user/addresses/detail",
    method: "GET",
    data: {
      addressId
    }
  });
};

const createUserAddress = (data) => {
  return request({
    url: "/user/addresses/create",
    method: "POST",
    data: data || {}
  });
};

const updateUserAddress = (data) => {
  return request({
    url: "/user/addresses/update",
    method: "POST",
    data: data || {}
  });
};

const deleteUserAddress = (addressId) => {
  return request({
    url: "/user/addresses/delete",
    method: "POST",
    data: {
      addressId
    }
  });
};

const setDefaultUserAddress = (addressId) => {
  return request({
    url: "/user/addresses/set-default",
    method: "POST",
    data: {
      addressId
    }
  });
};


const reverseGeocodeUserAddress = (data) => {
  return request({
    url: "/user/addresses/reverse-geocode",
    method: "GET",
    data: {
      latitude: data && data.latitude,
      longitude: data && data.longitude
    }
  });
};

module.exports = {
  listUserAddresses,
  getUserAddressDetail,
  createUserAddress,
  updateUserAddress,
  deleteUserAddress,
  setDefaultUserAddress,
  reverseGeocodeUserAddress
};
