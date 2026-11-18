import type { Metadata } from "next";
import localFont from "next/font/local";
import { GeistMono } from "geist/font/mono";
import { GeistSans } from "geist/font/sans";
import "./globals.css";
import Footer from "@/components/Footer";
import Navbar from "@/components/Navbar";

// Fonts are bundled from npm packages, not downloaded from Google at build time, so the build
// (and the Docker image build) works without internet access to a font service.
const bricolage = localFont({
  src: "../../node_modules/@fontsource-variable/bricolage-grotesque/files/bricolage-grotesque-latin-wght-normal.woff2",
  variable: "--font-bricolage",
  weight: "200 800",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "Movie Booking", template: "%s · Movie Booking" },
  description: "Find a movie, pick your seats and get your ticket in a minute.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en" className={`${GeistSans.variable} ${GeistMono.variable} ${bricolage.variable}`}>
      <body className="flex min-h-screen flex-col">
        <a
          href="#main"
          className="bg-brand sr-only rounded-md px-4 py-2 font-medium text-white focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50"
        >
          Skip to content
        </a>
        <Navbar />
        {children}
        <Footer />
      </body>
    </html>
  );
}
