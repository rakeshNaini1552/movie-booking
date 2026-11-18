import ButtonLink from "@/components/ButtonLink";
import PageContainer from "@/components/PageContainer";

export default function NotFound() {
  return (
    <PageContainer width="narrow">
      <div className="py-16 text-center">
        <p className="font-display text-brand-soft text-7xl font-extrabold">404</p>
        <p className="mt-4 mb-8 text-lg text-zinc-300">Page not found.</p>
        <ButtonLink href="/">Return home</ButtonLink>
      </div>
    </PageContainer>
  );
}
