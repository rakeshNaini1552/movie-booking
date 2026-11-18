import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import MoviePoster from "./MoviePoster";

// The poster is decorative (aria-hidden, empty alt), so these tests look at the markup directly.
describe("MoviePoster", () => {
  it("draws the generated poster with the title when there is no picture", () => {
    const { container } = render(<MoviePoster title="Inception" tag="PG-13" />);
    expect(container.querySelector("img")).toBeNull();
    expect(container).toHaveTextContent("Inception");
    expect(container).toHaveTextContent("PG-13");
  });

  it("shows the picture for a path on this site, and keeps the rating tag", () => {
    const { container } = render(
      <MoviePoster title="Inception" tag="PG-13" posterUrl="/posters/inception.jpg" />,
    );
    const image = container.querySelector("img");
    expect(image).not.toBeNull();
    expect(image?.getAttribute("src")).toContain(encodeURIComponent("/posters/inception.jpg"));
    expect(image).toHaveAttribute("alt", "");
    expect(container).toHaveTextContent("PG-13");
  });

  it.each([
    "https://example.com/inception.jpg",
    "//example.com/inception.jpg",
    "javascript:alert(1)",
  ])("falls back to the generated poster for %s", (posterUrl) => {
    const { container } = render(<MoviePoster title="Inception" posterUrl={posterUrl} />);
    expect(container.querySelector("img")).toBeNull();
    expect(container).toHaveTextContent("Inception");
  });

  it("ignores the picture in the compact booking-list version", () => {
    const { container } = render(
      <MoviePoster title="Inception" compact posterUrl="/posters/inception.jpg" />,
    );
    expect(container.querySelector("img")).toBeNull();
    expect(container).toHaveTextContent("I");
  });
});
