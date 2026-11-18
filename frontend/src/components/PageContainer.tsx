import type { ReactNode } from "react";

type PageContainerProps = {
  children: ReactNode;
  // "narrow" suits forms and single-column pages.
  width?: "default" | "narrow";
};

// One width and padding for every page, so pages do not set their own.
export default function PageContainer({ children, width = "default" }: PageContainerProps) {
  const max = width === "narrow" ? "max-w-3xl" : "max-w-6xl";
  return (
    <main id="main" className={`mx-auto w-full ${max} flex-1 px-4 py-10 sm:px-6`}>
      {children}
    </main>
  );
}
