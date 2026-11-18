import ButtonLink from "@/components/ButtonLink";

type PaginationProps = {
  previousHref: string | null;
  nextHref: string | null;
};

export default function Pagination({ previousHref, nextHref }: PaginationProps) {
  if (!previousHref && !nextHref) return null;
  return (
    <nav aria-label="Pagination" className="mt-10 flex justify-center gap-3">
      {previousHref && (
        <ButtonLink href={previousHref} rel="prev" variant="secondary">
          Previous
        </ButtonLink>
      )}
      {nextHref && (
        <ButtonLink href={nextHref} rel="next" variant="secondary">
          Next
        </ButtonLink>
      )}
    </nav>
  );
}
