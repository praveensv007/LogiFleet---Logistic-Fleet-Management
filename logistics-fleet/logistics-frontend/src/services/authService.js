import api, { unwrap, unwrapWithMessage } from './api';

const authService = {
  login: (body) => unwrap(api.post('/auth/login', body)),
  register: (body) => unwrap(api.post('/auth/register', body)),
  logout: () => api.post('/auth/logout').catch(() => null),
  forgotPassword: (email) => unwrapWithMessage(api.post('/auth/forgot-password', { email })),
  resetPassword: (body) => unwrapWithMessage(api.post('/auth/reset-password', body)),
  changePassword: (body) => unwrapWithMessage(api.put('/auth/change-password', body)),
  me: () => unwrap(api.get('/auth/me')),
  updateProfile: (body) => unwrap(api.put('/auth/me', body)),
  uploadFile: (file, category = 'document') => {
    const form = new FormData();
    form.append('file', file);
    form.append('category', category);
    return unwrap(api.post('/files/upload', form, { headers: { 'Content-Type': 'multipart/form-data' } }));
  },
};

export default authService;
