import { Navigate, Outlet } from 'react-router-dom';
import { FullPageLoading } from '../components/FullPageLoading';
import { ServiceUnreachable } from '../pages/ServiceUnreachable';
import { useAuth } from './AuthContext';

/**
 * Route guard for the admin shell.
 *
 * The guard is a routing convenience, not an authorization boundary: the API
 * rejects guests and non-admin sessions server-side. A 401 ends the local
 * session (see AuthProvider); a server that cannot be reached is recoverable
 * with an explicit retry.
 */
export function RequireAdmin() {
  const { status, bootError, retryBoot } = useAuth();

  if (status === 'checking') {
    return <FullPageLoading />;
  }

  if (status === 'unreachable') {
    return <ServiceUnreachable message={bootError} onRetry={retryBoot} />;
  }

  if (status === 'guest') {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
