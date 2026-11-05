import { useEffect, useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { logout } from '../../features/auth/api/authApi';
import { useAuthStore } from '../../features/auth/model/authStore';

// Run logout after leaving the member route, so clearing the session cannot
// trigger its access restriction while navigation to the home page is pending.
export function LogoutPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const started = useRef(false);
  const { mutate } = useMutation({
    mutationFn: logout,
    onSettled: () => {
      useAuthStore.getState().clearSession();
      queryClient.clear();
      navigate('/', { replace: true, state: null });
    },
  });

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    mutate();
  }, [mutate]);

  return <div className="site-shell"><main className="page-container" role="status">로그아웃 중…</main></div>;
}
