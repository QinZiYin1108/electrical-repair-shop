import { createApp } from 'vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './plugins/router';
import pinia from './plugins/pinia';
import './styles/transition.scss';

const app = createApp(App);

app.use(router);
app.use(pinia);
app.use(ElementPlus);

app.mount('#app');
