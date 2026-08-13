import { Outlet } from 'react-router-dom';
import { CustomerHeader } from './CustomerHeader';
import { Footer } from './Footer';
export function CustomerLayout() {
  return (
    <>
      <CustomerHeader />
      <main>
        <Outlet />
      </main>
      <Footer />
    </>
  );
}
