const HOME_TAB_URL = "/pages/home/index";

function markProgrammaticLeave(page) {
  if (page) {
    page._skipHomeRedirectOnUnload = true;
  }
}

function handleUnload(page) {
  if (page && page._skipHomeRedirectOnUnload) {
    page._skipHomeRedirectOnUnload = false;
    return;
  }
  wx.switchTab({
    url: HOME_TAB_URL
  });
}

function navigateBack(page) {
  markProgrammaticLeave(page);
  wx.navigateBack();
}

function redirectTo(page, url) {
  markProgrammaticLeave(page);
  wx.redirectTo({
    url
  });
}

function switchTabHome(page) {
  markProgrammaticLeave(page);
  wx.switchTab({
    url: HOME_TAB_URL
  });
}

module.exports = {
  handleUnload,
  navigateBack,
  redirectTo,
  switchTabHome
};
