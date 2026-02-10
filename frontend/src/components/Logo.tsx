import { cn } from "@/lib/utils";

interface LogoProps {
  className?: string;
  iconOnly?: boolean;
  size?: "sm" | "md" | "lg";
}

export function LogoIcon({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 100 100"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={cn("h-8 w-8", className)}
    >
      {/* Infinity symbol with arrows */}
      <path
        d="M25 50c0-8.3 6.7-15 15-15 5.5 0 10.3 3 12.9 7.4L60 50l-7.1 7.6C50.3 62 45.5 65 40 65c-8.3 0-15-6.7-15-15z"
        stroke="currentColor"
        strokeWidth="8"
        fill="none"
        strokeLinecap="round"
      />
      <path
        d="M75 50c0 8.3-6.7 15-15 15-5.5 0-10.3-3-12.9-7.4L40 50l7.1-7.6C49.7 38 54.5 35 60 35c8.3 0 15 6.7 15 15z"
        stroke="currentColor"
        strokeWidth="8"
        fill="none"
        strokeLinecap="round"
      />
      {/* Arrow pointing up-right */}
      <path
        d="M52 38l8-8m0 0v6m0-6h-6"
        stroke="currentColor"
        strokeWidth="6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      {/* Arrow pointing down-left */}
      <path
        d="M48 62l-8 8m0 0v-6m0 6h6"
        stroke="currentColor"
        strokeWidth="6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function Logo({ className, iconOnly = false, size = "md" }: LogoProps) {
  const sizeClasses = {
    sm: "h-6 w-6",
    md: "h-8 w-8",
    lg: "h-10 w-10",
  };

  const textSizeClasses = {
    sm: "text-lg",
    md: "text-xl",
    lg: "text-2xl",
  };

  return (
    <div className={cn("flex items-center gap-2", className)}>
      <LogoIcon className={sizeClasses[size]} />
      {!iconOnly && (
        <span className={cn("font-bold tracking-tight", textSizeClasses[size])}>
          SellSync
        </span>
      )}
    </div>
  );
}

export default Logo;
