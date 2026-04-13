import { createBrowserRouter, Navigate } from 'react-router';
import Login from './pages/Login';
import Register from './pages/Register';
import Chats from './pages/Chats';
import Chat from './pages/Chat';
import { AppProvider, useApp } from './context/AppContext';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user } = useApp();
  if (!user) {
    return <Navigate to="/" replace />;
  }
  return <>{children}</>;
}

function RootLayout({ children }: { children: React.ReactNode }) {
  return <AppProvider>{children}</AppProvider>;
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: (
      <RootLayout>
        <Login />
      </RootLayout>
    ),
  },
  {
    path: '/register',
    element: (
      <RootLayout>
        <Register />
      </RootLayout>
    ),
  },
  {
    path: '/chats',
    element: (
      <RootLayout>
        <ProtectedRoute>
          <Chats />
        </ProtectedRoute>
      </RootLayout>
    ),
  },
  {
    path: '/chat/:id',
    element: (
      <RootLayout>
        <ProtectedRoute>
          <Chat />
        </ProtectedRoute>
      </RootLayout>
    ),
  },
]);
