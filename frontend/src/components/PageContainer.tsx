import type { ReactNode } from "react";

type PageContainerProps = {
  children: ReactNode;
};

// One width and padding for every page, so pages do not set their own.
export default function PageContainer({ children }: PageContainerProps) {
  return <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8 sm:px-6">{children}</main>;
}
